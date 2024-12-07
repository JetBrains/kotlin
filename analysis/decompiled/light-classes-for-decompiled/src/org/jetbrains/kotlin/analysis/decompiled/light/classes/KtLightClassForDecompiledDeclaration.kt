/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.decompiled.light.classes

import com.intellij.openapi.util.Pair
import com.intellij.psi.*
import com.intellij.psi.impl.PsiClassImplUtil
import com.intellij.psi.impl.PsiImplUtil
import com.intellij.psi.impl.PsiSuperMethodImplUtil
import com.intellij.psi.javadoc.PsiDocComment
import com.intellij.psi.scope.PsiScopeProcessor
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.intellij.psi.util.PsiUtil
import org.jetbrains.annotations.NonNls
import org.jetbrains.kotlin.analysis.decompiled.light.classes.origin.LightMemberOriginForCompiledField
import org.jetbrains.kotlin.analysis.decompiled.light.classes.origin.LightMemberOriginForCompiledMethod
import org.jetbrains.kotlin.analysis.decompiler.psi.file.KtClsFile
import org.jetbrains.kotlin.asJava.KotlinAsJavaSupportBase
import org.jetbrains.kotlin.asJava.classes.*
import org.jetbrains.kotlin.asJava.isGetEntriesMethod
import org.jetbrains.kotlin.asJava.isSyntheticValuesOrValueOfMethod
import org.jetbrains.kotlin.load.java.structure.LightClassOriginKind
import org.jetbrains.kotlin.psi.KtClassOrObject

internal inline fun <R : PsiElement, T> R.cachedValueWithLibraryTracker(
    crossinline computer: () -> T,
): T = CachedValuesManager.getCachedValue(this) {
    CachedValueProvider.Result.createSingleDependency(
        computer(),
        KotlinAsJavaSupportBase.getInstance(project).librariesTracker(this),
    )
}

private inline fun <reified T> Collection<T>.toArrayIfNotEmptyOrDefault(default: Array<T>): Array<T> {
    return if (isNotEmpty()) toTypedArray() else default
}

open class KtLightClassForDecompiledDeclaration(
    clsDelegate: PsiClass,
    clsParent: PsiElement,
    protected val file: KtClsFile,
    kotlinOrigin: KtClassOrObject?
) : KtLightClassForDecompiledDeclarationBase(clsDelegate, clsParent, kotlinOrigin) {
    private val contentFinderCache by lazyPub {
        ClassContentFinderCache(
            extensibleClass = this,
            modificationTrackers = listOf(KotlinAsJavaSupportBase.getInstance(project).librariesTracker(this)),
        )
    }

    override fun getFields(): Array<PsiField> = ownFields.toArrayIfNotEmptyOrDefault(PsiField.EMPTY_ARRAY)
    override fun getMethods(): Array<PsiMethod> = ownMethods.toArrayIfNotEmptyOrDefault(PsiMethod.EMPTY_ARRAY)
    override fun getConstructors(): Array<PsiMethod> = ownConstructors.let { if (it.isEmpty()) it else it.clone() }
    override fun getInnerClasses(): Array<PsiClass> = ownInnerClasses.toArrayIfNotEmptyOrDefault(PsiClass.EMPTY_ARRAY)

    override fun findFieldByName(
        name: String,
        checkBases: Boolean,
    ): PsiField? = contentFinderCache.findFieldByName(name, checkBases)

    override fun findMethodsByName(
        name: String,
        checkBases: Boolean,
    ): Array<PsiMethod> = contentFinderCache.findMethodsByName(name, checkBases)

    override fun findInnerClassByName(
        name: String,
        checkBases: Boolean,
    ): PsiClass? = contentFinderCache.findInnerClassByName(name, checkBases)

    override fun hasModifierProperty(name: String): Boolean = clsDelegate.hasModifierProperty(name)

    override fun findMethodBySignature(patternMethod: PsiMethod, checkBases: Boolean): PsiMethod? =
        PsiClassImplUtil.findMethodBySignature(this, patternMethod, checkBases)

    override fun findMethodsBySignature(patternMethod: PsiMethod, checkBases: Boolean): Array<PsiMethod> =
        PsiClassImplUtil.findMethodsBySignature(this, patternMethod, checkBases)

    override fun findMethodsAndTheirSubstitutorsByName(@NonNls name: String, checkBases: Boolean): List<Pair<PsiMethod, PsiSubstitutor>> =
        PsiClassImplUtil.findMethodsAndTheirSubstitutorsByName(this, name, checkBases)

    override fun getImplementsList(): PsiReferenceList? = clsDelegate.implementsList

    override fun getRBrace(): PsiElement? = null

    override fun getLBrace(): PsiElement? = null

    override fun getInitializers(): Array<PsiClassInitializer> = clsDelegate.initializers

    override fun getContainingClass(): PsiClass? = parent as? PsiClass

    override fun isInheritorDeep(baseClass: PsiClass, classToByPass: PsiClass?): Boolean =
        clsDelegate.isInheritorDeep(baseClass, classToByPass)

    override fun getAllMethodsAndTheirSubstitutors(): List<Pair<PsiMethod?, PsiSubstitutor?>?> =
        PsiClassImplUtil.getAllWithSubstitutorsByMap<PsiMethod>(this, PsiClassImplUtil.MemberType.METHOD)

    override fun isInterface(): Boolean = clsDelegate.isInterface
    override fun getTypeParameters(): Array<PsiTypeParameter> = clsDelegate.typeParameters
    override fun isInheritor(baseClass: PsiClass, checkDeep: Boolean): Boolean = clsDelegate.isInheritor(baseClass, checkDeep)

    override fun processDeclarations(
        processor: PsiScopeProcessor,
        state: ResolveState,
        lastParent: PsiElement?,
        place: PsiElement
    ): Boolean = PsiClassImplUtil.processDeclarationsInClass(
        /* aClass = */ this,
        /* processor = */ processor,
        /* state = */ state,
        /* visited = */ null,
        /* last = */ lastParent,
        /* place = */ place,
        /* languageLevel = */ PsiUtil.getLanguageLevel(place),
        /* isRaw = */ false,
    )

    override fun isEnum(): Boolean = clsDelegate.isEnum
    override fun isRecord(): Boolean = clsDelegate.isRecord
    override fun getExtendsListTypes(): Array<PsiClassType> = PsiClassImplUtil.getExtendsListTypes(this)
    override fun getTypeParameterList(): PsiTypeParameterList? = clsDelegate.typeParameterList
    override fun isAnnotationType(): Boolean = clsDelegate.isAnnotationType
    override fun getNameIdentifier(): PsiIdentifier? = clsDelegate.nameIdentifier
    override fun getInterfaces(): Array<PsiClass> = PsiClassImplUtil.getInterfaces(this)
    override fun getSuperClass(): PsiClass? = PsiClassImplUtil.getSuperClass(this)
    override fun getSupers(): Array<PsiClass> = PsiClassImplUtil.getSupers(this)
    override fun getSuperTypes(): Array<PsiClassType> = PsiClassImplUtil.getSuperTypes(this)
    override fun getVisibleSignatures(): Collection<HierarchicalMethodSignature> = PsiSuperMethodImplUtil.getVisibleSignatures(this)
    override fun getQualifiedName(): String? = clsDelegate.qualifiedName
    override fun getImplementsListTypes(): Array<PsiClassType> = PsiClassImplUtil.getImplementsListTypes(this)
    override fun isDeprecated(): Boolean = clsDelegate.isDeprecated
    override fun setName(name: String): PsiElement = clsDelegate.setName(name)
    override fun hasTypeParameters(): Boolean = PsiImplUtil.hasTypeParameters(this)
    override fun getExtendsList(): PsiReferenceList? = clsDelegate.extendsList
    override fun getDocComment(): PsiDocComment? = clsDelegate.docComment
    override fun getModifierList(): PsiModifierList? = clsDelegate.modifierList
    override fun getScope(): PsiElement = clsDelegate.scope
    override fun getAllInnerClasses(): Array<PsiClass> = PsiClassImplUtil.getAllInnerClasses(this)
    override fun getAllMethods(): Array<PsiMethod> = PsiClassImplUtil.getAllMethods(this)
    override fun getAllFields(): Array<PsiField> = PsiClassImplUtil.getAllFields(this)

    private val ownConstructors: Array<PsiMethod>
        get() = cachedValueWithLibraryTracker {
            PsiImplUtil.getConstructors(this)
        }

    override fun getOwnMethods(): List<PsiMethod> = cachedValueWithLibraryTracker {
        val isEnum = isEnum
        this.clsDelegate.methods.mapNotNull { psiMethod ->
            // We replace cls method with generated ones to provide nullability annotations
            when {
                !isEnum -> {}
                isSyntheticValuesOrValueOfMethod(psiMethod) -> {
                    return@mapNotNull if (psiMethod.name == "valueOf") {
                        getEnumValueOfPsiMethod(this)
                    } else {
                        getEnumValuesPsiMethod(this)
                    }
                }

                isGetEntriesMethod(psiMethod) -> return@mapNotNull getEnumEntriesPsiMethod(this)
            }

            KtLightMethodForDecompiledDeclaration(
                funDelegate = psiMethod,
                funParent = this,
                lightMemberOrigin = LightMemberOriginForCompiledMethod(psiMethod, file)
            )
        }
    }

    override fun getOwnFields(): List<PsiField> = cachedValueWithLibraryTracker {
        this.clsDelegate.fields.map { psiField ->
            if (psiField is PsiEnumConstant) {
                KtLightEnumEntryForDecompiledDeclaration(
                    fldDelegate = psiField,
                    fldParent = this,
                    lightMemberOrigin = LightMemberOriginForCompiledField(psiField, file),
                    file = file,
                )
            } else {
                KtLightFieldForDecompiledDeclaration(
                    fldDelegate = psiField,
                    fldParent = this,
                    lightMemberOrigin = LightMemberOriginForCompiledField(psiField, file),
                )
            }
        }
    }

    override fun getOwnInnerClasses(): List<PsiClass> = cachedValueWithLibraryTracker {
        this.clsDelegate.innerClasses.map { psiClass ->
            val innerClassName = psiClass.name
            val innerDeclaration = this.kotlinOrigin
                ?.declarations
                ?.firstNotNullOfOrNull { clsDeclaration ->
                    (clsDeclaration as? KtClassOrObject)?.takeIf { it.name == innerClassName }
                }

            KtLightClassForDecompiledDeclaration(
                clsDelegate = psiClass,
                clsParent = this,
                file = file,
                kotlinOrigin = innerDeclaration,
            )
        }
    }

    override val originKind: LightClassOriginKind = LightClassOriginKind.BINARY
    override fun getNavigationElement() = kotlinOrigin?.navigationElement ?: file

    override fun equals(other: Any?): Boolean {
        return this === other || other is KtLightClassForDecompiledDeclaration &&
                qualifiedName == other.qualifiedName &&
                file == other.file
    }

    override fun hashCode(): Int = qualifiedName?.hashCode() ?: kotlinOrigin?.fqName?.hashCode() ?: 0
    override fun copy(): PsiElement = this
    override fun clone(): Any = this
    override fun toString(): String = "${this.javaClass.simpleName} of $parent"
    override fun getName(): String? = clsDelegate.name
    override fun isValid(): Boolean = file.isValid && clsDelegate.isValid && (kotlinOrigin?.isValid != false)
}