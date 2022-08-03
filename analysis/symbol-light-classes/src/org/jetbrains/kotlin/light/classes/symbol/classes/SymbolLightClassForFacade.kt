/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.light.classes.symbol.classes

import com.intellij.openapi.util.TextRange
import com.intellij.psi.*
import com.intellij.psi.impl.light.LightEmptyImplementsList
import com.intellij.psi.impl.light.LightModifierList
import org.jetbrains.annotations.NonNls
import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.scopes.KtScope
import org.jetbrains.kotlin.analysis.api.symbols.KtCallableSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtFunctionSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtKotlinPropertySymbol
import org.jetbrains.kotlin.analysis.api.symbols.markers.KtAnnotatedSymbol
import org.jetbrains.kotlin.analysis.api.symbols.markers.KtSymbolWithVisibility
import org.jetbrains.kotlin.asJava.classes.KtLightClassForFacade
import org.jetbrains.kotlin.asJava.classes.lazyPub
import org.jetbrains.kotlin.asJava.elements.FakeFileForLightClass
import org.jetbrains.kotlin.asJava.elements.KtLightField
import org.jetbrains.kotlin.asJava.elements.KtLightMethod
import org.jetbrains.kotlin.descriptors.annotations.AnnotationUseSiteTarget
import org.jetbrains.kotlin.fileClasses.javaFileFacadeFqName
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.light.classes.symbol.NullabilityType
import org.jetbrains.kotlin.light.classes.symbol.annotations.computeAnnotations
import org.jetbrains.kotlin.light.classes.symbol.annotations.hasInlineOnlyAnnotation
import org.jetbrains.kotlin.light.classes.symbol.annotations.hasJvmFieldAnnotation
import org.jetbrains.kotlin.light.classes.symbol.annotations.hasJvmMultifileClassAnnotation
import org.jetbrains.kotlin.light.classes.symbol.fields.SymbolLightField
import org.jetbrains.kotlin.light.classes.symbol.modifierLists.SymbolLightClassModifierList
import org.jetbrains.kotlin.light.classes.symbol.toPsiVisibilityForMember
import org.jetbrains.kotlin.load.java.structure.LightClassOriginKind
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtFile

context(KtAnalysisSession)
class SymbolLightClassForFacade(
    override val facadeClassFqName: FqName,
    override val files: Collection<KtFile>
) : SymbolLightClassBase(files.first().manager), KtLightClassForFacade {

    init {
        require(files.isNotEmpty())
        /*
        Actually, here should be the following check
        require(files.all { it.getKtModule() is KtSourceModule })
        but it is quite expensive
         */
        require(files.none { it.isCompiled })
    }

    private val firstFileInFacade by lazyPub { files.first() }

    private val fileSymbols by lazyPub {
        files.map { ktFile ->
            ktFile.getFileSymbol()
        }
    }

    private val _modifierList: PsiModifierList by lazyPub {
        if (multiFileClass)
            return@lazyPub LightModifierList(manager, KotlinLanguage.INSTANCE, PsiModifier.PUBLIC, PsiModifier.FINAL)

        val modifiers = setOf(PsiModifier.PUBLIC, PsiModifier.FINAL)

        val annotations = fileSymbols.flatMap {
            it.computeAnnotations(
                this@SymbolLightClassForFacade,
                NullabilityType.Unknown,
                AnnotationUseSiteTarget.FILE,
                includeAnnotationsWithoutSite = false
            )
        }

        SymbolLightClassModifierList(this@SymbolLightClassForFacade, modifiers, annotations)
    }

    override fun getModifierList(): PsiModifierList = _modifierList

    override fun getScope(): PsiElement = parent

    private val _ownMethods: List<KtLightMethod> by lazyPub {
        val result = mutableListOf<KtLightMethod>()

        val methodsAndProperties = sequence<KtCallableSymbol> {
            for (fileSymbol in fileSymbols) {
                for (callableSymbol in fileSymbol.getFileScope().getCallableSymbols()) {
                    if (callableSymbol !is KtFunctionSymbol && callableSymbol !is KtKotlinPropertySymbol) continue
                    if (callableSymbol !is KtSymbolWithVisibility) continue
                    if ((callableSymbol as? KtAnnotatedSymbol)?.hasInlineOnlyAnnotation() == true) continue
                    val isPrivate = callableSymbol.toPsiVisibilityForMember() == PsiModifier.PRIVATE
                    if (isPrivate && multiFileClass) continue
                    yield(callableSymbol)
                }
            }
        }
        createMethods(methodsAndProperties, result, isTopLevel = true)

        result
    }

    private val multiFileClass: Boolean by lazyPub {
        files.size > 1 || fileSymbols.any { it.hasJvmMultifileClassAnnotation() }
    }

    private fun loadFieldsFromFile(
        fileScope: KtScope,
        nameGenerator: SymbolLightField.FieldNameGenerator,
        result: MutableList<KtLightField>
    ) {
        for (propertySymbol in fileScope.getCallableSymbols()) {

            if (propertySymbol !is KtKotlinPropertySymbol) continue

            // If this facade represents multiple files, only `const` properties need to be generated.
            if (multiFileClass && !propertySymbol.isConst) continue

            val isLateInitWithPublicAccessors = if (propertySymbol.isLateInit) {
                val getterIsPublic = propertySymbol.getter?.toPsiVisibilityForMember()
                    ?.let { it == PsiModifier.PUBLIC } ?: true
                val setterIsPublic = propertySymbol.setter?.toPsiVisibilityForMember()
                    ?.let { it == PsiModifier.PUBLIC } ?: true
                getterIsPublic && setterIsPublic
            } else false

            val forceStaticAndPropertyVisibility = isLateInitWithPublicAccessors ||
                    (propertySymbol.isConst) ||
                    propertySymbol.hasJvmFieldAnnotation()

            createField(
                propertySymbol,
                nameGenerator,
                isTopLevel = true,
                forceStatic = forceStaticAndPropertyVisibility,
                takePropertyVisibility = forceStaticAndPropertyVisibility,
                result
            )
        }

    }

    private val _ownFields: List<KtLightField> by lazyPub {
        val result = mutableListOf<KtLightField>()
        val nameGenerator = SymbolLightField.FieldNameGenerator()
        for (fileSymbol in fileSymbols) {
            loadFieldsFromFile(fileSymbol.getFileScope(), nameGenerator, result)
        }
        result
    }

    override fun getOwnFields() = _ownFields

    override fun getOwnMethods() = _ownMethods

    override fun copy(): SymbolLightClassForFacade = SymbolLightClassForFacade(facadeClassFqName, files)

    private val packageFqName: FqName = facadeClassFqName.parent()

    private val modifierList: PsiModifierList = LightModifierList(manager, KotlinLanguage.INSTANCE, PsiModifier.PUBLIC, PsiModifier.FINAL)

    private val implementsList: LightEmptyImplementsList = LightEmptyImplementsList(manager)

    private val packageClsFile = FakeFileForLightClass(
        firstFileInFacade,
        lightClass = { this },
        packageFqName = packageFqName
    )

    override fun getParent(): PsiElement = containingFile

    override val kotlinOrigin: KtClassOrObject? get() = null

    val fqName: FqName
        get() = facadeClassFqName

    override fun hasModifierProperty(@NonNls name: String) = modifierList.hasModifierProperty(name)

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

    override fun getImplementsList() = implementsList

    override fun getInterfaces(): Array<out PsiClass> = PsiClass.EMPTY_ARRAY

    override fun getInnerClasses(): Array<out PsiClass> = PsiClass.EMPTY_ARRAY

    override fun getOwnInnerClasses(): List<PsiClass> = listOf()

    override fun getAllInnerClasses(): Array<PsiClass> = PsiClass.EMPTY_ARRAY

    override fun findInnerClassByName(@NonNls name: String, checkBases: Boolean): PsiClass? = null

    override fun isInheritorDeep(baseClass: PsiClass?, classToByPass: PsiClass?): Boolean = false

    override fun getName() = super<KtLightClassForFacade>.getName()

    override fun getQualifiedName() = facadeClassFqName.asString()

    override fun getNameIdentifier(): PsiIdentifier? = null

    override fun isValid() = files.all { it.isValid && it.hasTopLevelCallables() && facadeClassFqName == it.javaFileFacadeFqName }

    override fun getNavigationElement() = firstFileInFacade

    override fun isEquivalentTo(another: PsiElement?): Boolean =
        equals(another) || another is SymbolLightClassForFacade && another.qualifiedName == qualifiedName

    override fun isInheritor(baseClass: PsiClass, checkDeep: Boolean): Boolean {
        return baseClass.qualifiedName == CommonClassNames.JAVA_LANG_OBJECT
    }

    override fun getSuperClass(): PsiClass? {
        return JavaPsiFacade.getInstance(project).findClass(CommonClassNames.JAVA_LANG_OBJECT, resolveScope)
    }

    override fun getSupers(): Array<PsiClass> =
        superClass?.let { arrayOf(it) } ?: arrayOf()

    override fun getSuperTypes(): Array<PsiClassType> =
        arrayOf(PsiType.getJavaLangObject(manager, resolveScope))

    override fun equals(other: Any?): Boolean {
        if (other !is SymbolLightClassForFacade) return false
        if (this === other) return true

        if (this.hashCode() != other.hashCode()) return false
        if (manager != other.manager) return false
        if (facadeClassFqName != other.facadeClassFqName) return false
        if (!fileSymbols.containsAll(other.fileSymbols)) return false
        if (!other.fileSymbols.containsAll(fileSymbols)) return false
        return true
    }

    override fun hashCode() = facadeClassFqName.hashCode()

    override fun toString() = "${SymbolLightClassForFacade::class.java.simpleName}:$facadeClassFqName"

    override val originKind: LightClassOriginKind
        get() = LightClassOriginKind.SOURCE

    override fun getText() = firstFileInFacade.text ?: ""

    override fun getTextRange(): TextRange = firstFileInFacade.textRange ?: TextRange.EMPTY_RANGE

    override fun getTextOffset() = firstFileInFacade.textOffset

    override fun getStartOffsetInParent() = firstFileInFacade.startOffsetInParent

    override fun isWritable() = files.all { it.isWritable }
}
