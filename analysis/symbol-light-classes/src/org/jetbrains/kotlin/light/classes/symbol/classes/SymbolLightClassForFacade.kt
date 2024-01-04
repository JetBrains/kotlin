/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.light.classes.symbol.classes

import com.intellij.openapi.util.TextRange
import com.intellij.psi.*
import com.intellij.psi.impl.light.LightEmptyImplementsList
import org.jetbrains.annotations.NonNls
import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.scopes.KtScope
import org.jetbrains.kotlin.analysis.api.symbols.*
import org.jetbrains.kotlin.analysis.api.symbols.markers.KtAnnotatedSymbol
import org.jetbrains.kotlin.analysis.api.symbols.markers.KtPossibleMultiplatformSymbol
import org.jetbrains.kotlin.analysis.api.symbols.markers.KtSymbolWithVisibility
import org.jetbrains.kotlin.analysis.api.symbols.pointers.symbolPointerOfType
import org.jetbrains.kotlin.analysis.project.structure.KtModule
import org.jetbrains.kotlin.asJava.classes.KtLightClassForFacade
import org.jetbrains.kotlin.asJava.classes.lazyPub
import org.jetbrains.kotlin.asJava.elements.FakeFileForLightClass
import org.jetbrains.kotlin.asJava.elements.KtLightField
import org.jetbrains.kotlin.asJava.elements.KtLightMethod
import org.jetbrains.kotlin.descriptors.annotations.AnnotationUseSiteTarget
import org.jetbrains.kotlin.fileClasses.isJvmMultifileClassFile
import org.jetbrains.kotlin.fileClasses.javaFileFacadeFqName
import org.jetbrains.kotlin.light.classes.symbol.analyzeForLightClasses
import org.jetbrains.kotlin.light.classes.symbol.annotations.*
import org.jetbrains.kotlin.light.classes.symbol.cachedValue
import org.jetbrains.kotlin.light.classes.symbol.fields.SymbolLightField
import org.jetbrains.kotlin.light.classes.symbol.modifierLists.InitializedModifiersBox
import org.jetbrains.kotlin.light.classes.symbol.modifierLists.SymbolLightClassModifierList
import org.jetbrains.kotlin.light.classes.symbol.toPsiVisibilityForMember
import org.jetbrains.kotlin.load.java.structure.LightClassOriginKind
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtFile

internal class SymbolLightClassForFacade(
    override val facadeClassFqName: FqName,
    override val files: Collection<KtFile>,
    ktModule: KtModule,
) : SymbolLightClassBase(ktModule, files.first().manager), KtLightClassForFacade {

    init {
        require(files.isNotEmpty())
        /*
        Actually, here should be the following check
        require(files.all { it.getKtModule() is KtSourceModule })
        but it is quite expensive
         */
        require(files.none { it.isCompiled })
    }

    private fun <T> withFileSymbols(action: KtAnalysisSession.(List<KtFileSymbol>) -> T): T =
        analyzeForLightClasses(ktModule) {
            action(files.map { it.getFileSymbol() })
        }

    private val firstFileInFacade: KtFile get() = files.first()

    private val _modifierList: PsiModifierList by lazyPub {
        SymbolLightClassModifierList(
            containingDeclaration = this,
            modifiersBox = InitializedModifiersBox(PsiModifier.PUBLIC, PsiModifier.FINAL),
            annotationsBox = if (multiFileClass) {
                EmptyAnnotationsBox
            } else {
                GranularAnnotationsBox(
                    annotationsProvider = SymbolAnnotationsProvider(
                        ktModule = this.ktModule,
                        annotatedSymbolPointer = firstFileInFacade.symbolPointerOfType<KtFileSymbol>(),
                        annotationUseSiteTargetFilter = AnnotationUseSiteTarget.FILE.toOptionalFilter(),
                    )
                )
            },
        )
    }

    override fun getModifierList(): PsiModifierList = _modifierList

    override fun getScope(): PsiElement = parent

    override fun getOwnMethods(): List<PsiMethod> = cachedValue {
        withFileSymbols { fileSymbols ->
            val result = mutableListOf<KtLightMethod>()
            val methodsAndProperties = sequence<KtCallableSymbol> {
                for (fileSymbol in fileSymbols) {
                    for (callableSymbol in fileSymbol.getFileScope().getCallableSymbols()) {
                        if (callableSymbol !is KtFunctionSymbol && callableSymbol !is KtKotlinPropertySymbol) continue
                        @Suppress("USELESS_IS_CHECK") // K2 warning suppression, TODO: KT-62472
                        if (callableSymbol !is KtSymbolWithVisibility) continue

                        // We shouldn't materialize expect declarations
                        @Suppress("USELESS_IS_CHECK") // K2 warning suppression, TODO: KT-62472
                        if (callableSymbol is KtPossibleMultiplatformSymbol && callableSymbol.isExpect) continue
                        if ((callableSymbol as? KtAnnotatedSymbol)?.hasInlineOnlyAnnotation() == true) continue
                        if (multiFileClass && callableSymbol.toPsiVisibilityForMember() == PsiModifier.PRIVATE) continue
                        if (callableSymbol.hasTypeForValueClassInSignature(ignoreReturnType = true)) continue
                        yield(callableSymbol)
                    }
                }
            }

            createMethods(methodsAndProperties, result, isTopLevel = true)
            result
        }
    }

    override val multiFileClass: Boolean get() = files.size > 1 || firstFileInFacade.isJvmMultifileClassFile

    context(KtAnalysisSession)
    private fun loadFieldsFromFile(
        fileScope: KtScope,
        nameGenerator: SymbolLightField.FieldNameGenerator,
        result: MutableList<KtLightField>
    ) {
        for (propertySymbol in fileScope.getCallableSymbols()) {
            if (propertySymbol !is KtKotlinPropertySymbol) continue

            // If this facade represents multiple files, only `const` properties need to be generated.
            if (multiFileClass && !propertySymbol.isConst) continue

            createField(
                propertySymbol,
                nameGenerator,
                isStatic = true,
                result,
            )
        }
    }

    private fun KtPropertyAccessorSymbol?.isNullOrPublic(): Boolean =
        this?.toPsiVisibilityForMember()?.let { it == PsiModifier.PUBLIC } != false

    override fun getOwnFields(): List<PsiField> = cachedValue {
        val result = mutableListOf<KtLightField>()
        val nameGenerator = SymbolLightField.FieldNameGenerator()
        withFileSymbols { fileSymbols ->
            for (fileSymbol in fileSymbols) {
                loadFieldsFromFile(fileSymbol.getFileScope(), nameGenerator, result)
            }
        }

        result
    }

    override fun copy(): SymbolLightClassForFacade = SymbolLightClassForFacade(facadeClassFqName, files, ktModule)

    private val packageClsFile = FakeFileForLightClass(
        firstFileInFacade,
        lightClass = this,
        packageFqName = facadeClassFqName.parent()
    )

    override fun getParent(): PsiElement = containingFile
    override val kotlinOrigin: KtClassOrObject? get() = null
    override fun hasModifierProperty(@NonNls name: String) = name == PsiModifier.PUBLIC || name == PsiModifier.FINAL
    override fun getExtendsList(): PsiReferenceList? = null
    override fun isDeprecated() = false
    override fun isInterface() = false
    override fun isAnnotationType() = false
    override fun isEnum() = false
    override fun getContainingClass(): PsiClass? = null

    override fun getContainingFile() = packageClsFile
    override fun hasTypeParameters() = false
    override fun getTypeParameters(): Array<out PsiTypeParameter> = PsiTypeParameter.EMPTY_ARRAY
    override fun getTypeParameterList(): PsiTypeParameterList? = null
    override fun getImplementsList(): LightEmptyImplementsList = object : LightEmptyImplementsList(manager) {
        override fun getParent(): PsiElement = this@SymbolLightClassForFacade
    }

    override fun getInterfaces(): Array<out PsiClass> = PsiClass.EMPTY_ARRAY
    override fun getInnerClasses(): Array<out PsiClass> = PsiClass.EMPTY_ARRAY
    override fun getOwnInnerClasses(): List<PsiClass> = listOf()
    override fun getAllInnerClasses(): Array<PsiClass> = PsiClass.EMPTY_ARRAY
    override fun findInnerClassByName(@NonNls name: String, checkBases: Boolean): PsiClass? = null
    override fun isInheritorDeep(baseClass: PsiClass?, classToByPass: PsiClass?): Boolean = false
    override fun getName(): String = super<KtLightClassForFacade>.getName()
    override fun getQualifiedName(): String = facadeClassFqName.asString()
    override fun getNameIdentifier(): PsiIdentifier? = null

    override fun isValid() = files.all {
        it.isValid && facadeClassFqName == it.javaFileFacadeFqName
    } && files.any {
        it.hasTopLevelCallables()
    }

    override fun getNavigationElement() = firstFileInFacade

    override fun isEquivalentTo(another: PsiElement?): Boolean =
        equals(another) || another is SymbolLightClassForFacade && another.qualifiedName == qualifiedName

    override fun isInheritor(baseClass: PsiClass, checkDeep: Boolean): Boolean {
        return baseClass.qualifiedName == CommonClassNames.JAVA_LANG_OBJECT
    }

    override fun getSuperClass(): PsiClass? {
        return JavaPsiFacade.getInstance(project).findClass(CommonClassNames.JAVA_LANG_OBJECT, resolveScope)
    }

    override fun getSupers(): Array<PsiClass> = superClass?.let { arrayOf(it) } ?: PsiClass.EMPTY_ARRAY
    override fun getSuperTypes(): Array<PsiClassType> = arrayOf(PsiType.getJavaLangObject(manager, resolveScope))

    override fun equals(other: Any?): Boolean {
        return this === other || other is SymbolLightClassForFacade &&
                facadeClassFqName == other.facadeClassFqName &&
                files == other.files
    }

    override fun hashCode() = facadeClassFqName.hashCode()
    override fun toString() = "${SymbolLightClassForFacade::class.java.simpleName}:$facadeClassFqName"
    override val originKind: LightClassOriginKind get() = LightClassOriginKind.SOURCE
    override fun getText() = firstFileInFacade.text ?: ""
    override fun getTextRange(): TextRange = firstFileInFacade.textRange ?: TextRange.EMPTY_RANGE
    override fun getTextOffset() = firstFileInFacade.textOffset
    override fun getStartOffsetInParent() = firstFileInFacade.startOffsetInParent
    override fun isWritable() = files.all { it.isWritable }
}
