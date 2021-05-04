/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.asJava

import com.intellij.openapi.util.Comparing
import com.intellij.openapi.util.TextRange
import com.intellij.psi.*
import com.intellij.psi.impl.light.LightEmptyImplementsList
import com.intellij.psi.impl.light.LightModifierList
import org.jetbrains.annotations.NonNls
import org.jetbrains.kotlin.asJava.classes.KtLightClassForFacadeImpl
import org.jetbrains.kotlin.asJava.classes.lazyPub
import org.jetbrains.kotlin.asJava.elements.*
import org.jetbrains.kotlin.descriptors.annotations.AnnotationUseSiteTarget
import org.jetbrains.kotlin.fileClasses.javaFileFacadeFqName
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.idea.asJava.classes.analyseForLightClasses
import org.jetbrains.kotlin.idea.asJava.classes.createField
import org.jetbrains.kotlin.idea.asJava.classes.createMethods
import org.jetbrains.kotlin.idea.frontend.api.analyse
import org.jetbrains.kotlin.idea.frontend.api.fir.analyzeWithSymbolAsContext
import org.jetbrains.kotlin.idea.frontend.api.scopes.KtDeclarationScope
import org.jetbrains.kotlin.idea.frontend.api.symbols.KtCallableSymbol
import org.jetbrains.kotlin.idea.frontend.api.symbols.KtKotlinPropertySymbol
import org.jetbrains.kotlin.idea.frontend.api.symbols.KtFunctionSymbol
import org.jetbrains.kotlin.idea.frontend.api.symbols.markers.KtSymbolWithDeclarations
import org.jetbrains.kotlin.idea.frontend.api.symbols.markers.KtSymbolWithVisibility
import org.jetbrains.kotlin.load.java.structure.LightClassOriginKind
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtFile

class FirLightClassForFacade(
    manager: PsiManager,
    private val facadeClassFqName: FqName,
    private val files: Collection<KtFile>
) : FirLightClassBase(manager) {

    init {
        require(files.isNotEmpty())
    }

    private val firstFileInFacade by lazyPub { files.first() }

    override val clsDelegate: PsiClass get() = invalidAccess()

    private val fileSymbols by lazyPub {
        files.map { ktFile ->
            analyseForLightClasses(ktFile) {
                ktFile.getFileSymbol()
            }
        }
    }

    private val _modifierList: PsiModifierList by lazyPub {
        if (multiFileClass)
            return@lazyPub LightModifierList(manager, KotlinLanguage.INSTANCE, PsiModifier.PUBLIC, PsiModifier.FINAL)

        val modifiers = setOf(PsiModifier.PUBLIC, PsiModifier.FINAL)

        val annotations = fileSymbols.flatMap {
            it.computeAnnotations(
                this@FirLightClassForFacade,
                NullabilityType.Unknown,
                AnnotationUseSiteTarget.FILE,
                includeAnnotationsWithoutSite = false
            )
        }

        FirLightClassModifierList(this@FirLightClassForFacade, modifiers, annotations)
    }

    override fun getModifierList(): PsiModifierList = _modifierList

    override fun getScope(): PsiElement = parent

    private val _ownMethods: List<KtLightMethod> by lazyPub {
        val result = mutableListOf<KtLightMethod>()


        val methodsAndProperties = sequence<KtCallableSymbol> {
            for (fileSymbol in fileSymbols) {
                analyzeWithSymbolAsContext(fileSymbol) {
                    for (callableSymbol in fileSymbol.getFileScope().getCallableSymbols()) {
                        if (callableSymbol !is KtFunctionSymbol && callableSymbol !is KtKotlinPropertySymbol) continue
                        if (callableSymbol !is KtSymbolWithVisibility) continue
                        val isPrivate = callableSymbol.toPsiVisibilityForMember(isTopLevel = true) == PsiModifier.PRIVATE
                        if (isPrivate && multiFileClass) continue
                        yield(callableSymbol)
                    }
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
        fileScope: KtDeclarationScope<KtSymbolWithDeclarations>,
        nameGenerator: FirLightField.FieldNameGenerator,
        result: MutableList<KtLightField>
    ) {
        for (propertySymbol in fileScope.getCallableSymbols()) {

            if (propertySymbol !is KtKotlinPropertySymbol) continue

            if (propertySymbol.isConst && multiFileClass) continue

            val isLateInitWithPublicAccessors = if (propertySymbol.isLateInit) {
                val getterIsPublic = propertySymbol.getter?.toPsiVisibilityForMember(isTopLevel = true)
                    ?.let { it == PsiModifier.PUBLIC } ?: true
                val setterIsPublic = propertySymbol.setter?.toPsiVisibilityForMember(isTopLevel = true)
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
        val nameGenerator = FirLightField.FieldNameGenerator()
        for (fileSymbol in fileSymbols) {
            analyzeWithSymbolAsContext(fileSymbol) {
                loadFieldsFromFile(fileSymbol.getFileScope(), nameGenerator, result)
            }
        }
        result
    }

    override fun getOwnFields() = _ownFields

    override fun getOwnMethods() = _ownMethods

    override fun copy(): FirLightClassForFacade =
        FirLightClassForFacade(manager, facadeClassFqName, files)

    private val packageFqName: FqName =
        facadeClassFqName.parent()

    private val modifierList: PsiModifierList =
        LightModifierList(manager, KotlinLanguage.INSTANCE, PsiModifier.PUBLIC, PsiModifier.FINAL)

    private val implementsList: LightEmptyImplementsList =
        LightEmptyImplementsList(manager)

    private val packageClsFile = FakeFileForLightClass(
        firstFileInFacade,
        lightClass = { this },
        stub = { null },
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

    override fun getName(): String = facadeClassFqName.shortName().asString()

    override fun getQualifiedName() = facadeClassFqName.asString()

    override fun getNameIdentifier(): PsiIdentifier? = null

    override fun isValid() = files.all { it.isValid && it.hasTopLevelCallables() && facadeClassFqName == it.javaFileFacadeFqName }

    override fun getNavigationElement() = firstFileInFacade

    override fun isEquivalentTo(another: PsiElement?): Boolean =
        equals(another) || another is FirLightClassForFacade && Comparing.equal(another.qualifiedName, qualifiedName)

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
        if (other !is FirLightClassForFacade) return false
        if (this === other) return true

        if (this.hashCode() != other.hashCode()) return false
        if (manager != other.manager) return false
        if (facadeClassFqName != other.facadeClassFqName) return false
        if (!fileSymbols.containsAll(other.fileSymbols)) return false
        if (!other.fileSymbols.containsAll(fileSymbols)) return false
        return true
    }

    override fun hashCode() = facadeClassFqName.hashCode()

    override fun toString() = "${KtLightClassForFacadeImpl::class.java.simpleName}:$facadeClassFqName"

    override val originKind: LightClassOriginKind
        get() = LightClassOriginKind.SOURCE

    override fun getText() = firstFileInFacade.text ?: ""

    override fun getTextRange(): TextRange = firstFileInFacade.textRange ?: TextRange.EMPTY_RANGE

    override fun getTextOffset() = firstFileInFacade.textOffset

    override fun getStartOffsetInParent() = firstFileInFacade.startOffsetInParent

    override fun isWritable() = files.all { it.isWritable }
}
