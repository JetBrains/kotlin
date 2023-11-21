/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.asJava

import com.intellij.openapi.util.NlsSafe
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiField
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiNamedElement
import com.intellij.psi.impl.java.stubs.PsiClassStub
import com.intellij.psi.stubs.PsiFileStub
import com.intellij.psi.stubs.StubElement
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.asJava.classes.KtLightClass
import org.jetbrains.kotlin.asJava.elements.KtLightElement
import org.jetbrains.kotlin.asJava.elements.KtLightField
import org.jetbrains.kotlin.asJava.elements.KtLightMethod
import org.jetbrains.kotlin.asJava.elements.isSetter
import org.jetbrains.kotlin.descriptors.annotations.AnnotationUseSiteTarget
import org.jetbrains.kotlin.load.java.JvmAbi
import org.jetbrains.kotlin.name.JvmStandardClassIds
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.containingClassOrObject
import org.jetbrains.kotlin.resolve.DataClassResolver
import org.jetbrains.kotlin.utils.checkWithAttachment

object LightClassUtil {

    fun findClass(stub: StubElement<*>, predicate: (PsiClassStub<*>) -> Boolean): PsiClass? {
        if (stub is PsiClassStub<*> && predicate(stub)) {
            return stub.psi
        }

        if (stub is PsiClassStub<*> || stub is PsiFileStub<*>) {
            for (child in stub.childrenStubs) {
                val answer = findClass(child, predicate)
                if (answer != null) return answer
            }
        }
        return null
    }

    fun getLightClassAccessorMethod(accessor: KtPropertyAccessor): PsiMethod? =
        getLightClassAccessorMethods(accessor).firstOrNull()

    fun getLightClassAccessorMethods(accessor: KtPropertyAccessor): List<PsiMethod> {
        val property = accessor.property
        val customNameAnnoProvided =
            accessor.annotationEntries.find { JvmStandardClassIds.JVM_NAME.shortName() == it.shortName } != null || property.isSpecialNameProvided()
        val propertyName = accessor.property.name ?: return emptyList()
        val wrappers = getPsiMethodWrappers(property) { wrapper ->
            val wrapperName = wrapper.name
            if (customNameAnnoProvided) {
                // cls with loaded text doesn't preserve annotation arguments thus we can't rely on the name
                // accept everything but the the opposite accessors
                if (accessor.isGetter) !JvmAbi.isSetterName(wrapperName) else !JvmAbi.isGetterName(wrapperName)
            } else if (accessor.isGetter) {
                val getterName = JvmAbi.getterName(propertyName)
                wrapperName == getterName ||
                        isMangled(wrapperName, getterName)
            } else {
                val setterName = JvmAbi.setterName(propertyName)
                wrapperName == setterName || isMangled(wrapperName, setterName)
            }
        }
        return wrappers.toList()
    }

    private fun isMangled(wrapperName: @NlsSafe String, prefix: String): Boolean {
        //see KT-54803 for other mangling strategies
        // A memory optimization for `wrapperName.startsWith("$prefix$")`, see KT-63486
        return wrapperName.length > prefix.length
                && wrapperName[prefix.length] == '$'
                && wrapperName.startsWith(prefix)
    }

    fun getLightFieldForCompanionObject(companionObject: KtClassOrObject): PsiField? {
        val outerPsiClass = getWrappingClass(companionObject)
        if (outerPsiClass != null) {
            for (fieldOfParent in outerPsiClass.fields) {
                if ((fieldOfParent is KtLightElement<*, *>) && fieldOfParent.kotlinOrigin === companionObject.originalElement) {
                    return fieldOfParent
                }
            }
        }
        return null
    }

    fun getLightClassPropertyMethods(property: KtProperty): PropertyAccessorsPsiMethods {
        val getter = property.getter
        val setter = property.setter

        val getterWrapper = if (getter != null) getLightClassAccessorMethod(getter) else null
        val setterWrapper = if (setter != null) getLightClassAccessorMethod(setter) else null

        return extractPropertyAccessors(property, getterWrapper, setterWrapper)
    }

    fun getLightClassBackingField(declaration: KtDeclaration): PsiField? {
        var psiClass: PsiClass = getWrappingClass(declaration) ?: return null
        var fieldFinder: (PsiField) -> Boolean = { psiField ->
            psiField is KtLightElement<*, *> && psiField.kotlinOrigin === declaration
        }

        if (psiClass is KtLightClass) {
            val origin = psiClass.kotlinOrigin
            if (origin is KtObjectDeclaration && origin.isCompanion()) {
                val containingClass = PsiTreeUtil.getParentOfType(origin, KtClass::class.java)
                if (containingClass != null) {
                    val containingLightClass = containingClass.toLightClass()
                    if (containingLightClass != null) {
                        psiClass = containingLightClass
                    }
                }
            } else {
                // Decompiled version of [KtLightClass] may not have [kotlinOrigin]
                val containingDeclaration = declaration.containingClassOrObject
                if (containingDeclaration is KtObjectDeclaration &&
                    containingDeclaration.isCompanion()
                ) {
                    psiClass.containingClass?.let { containingClass ->
                        psiClass = containingClass
                        fieldFinder = { psiField ->
                            psiField.name == declaration.name
                        }
                    }
                }
            }
        }

        return psiClass.fields.find(fieldFinder)
    }

    fun getLightClassPropertyMethods(parameter: KtParameter): PropertyAccessorsPsiMethods {
        return extractPropertyAccessors(parameter, null, null)
    }

    fun getLightClassMethod(function: KtFunction): PsiMethod? {
        return getPsiMethodWrapper(function)
    }

    /**
     * Returns the light method generated from the parameter of an annotation class.
     */
    fun getLightClassMethod(parameter: KtParameter): PsiMethod? {
        return getPsiMethodWrapper(parameter)
    }

    fun getLightClassMethods(function: KtFunction): List<PsiMethod> {
        return getPsiMethodWrappers(function).toList()
    }

    fun getLightClassMethodsByName(function: KtFunction, name: String): Sequence<KtLightMethod> {
        return getPsiMethodWrappers(function, name)
    }

    private fun getPsiMethodWrapper(declaration: KtDeclaration): PsiMethod? {
        return getPsiMethodWrappers(declaration).firstOrNull()
    }

    private fun getPsiMethodWrappers(
        declaration: KtDeclaration,
        name: String? = null,
        nameFilter: (KtLightMethod) -> Boolean = { name == null || name == it.name }
    ): Sequence<KtLightMethod> =
        getWrappingClasses(declaration).flatMap { it.methods.asSequence() }
            .filterIsInstance<KtLightMethod>()
            .filter(nameFilter)
            .filter { it.kotlinOrigin === declaration || it.navigationElement === declaration }

    private fun getWrappingClass(declaration: KtDeclaration): PsiClass? {
        if (declaration is KtParameter) {
            val constructorClass = KtPsiUtil.getClassIfParameterIsProperty(declaration)
            if (constructorClass != null) {
                return constructorClass.toLightClass()
            }
        }

        var ktDeclaration = declaration
        if (ktDeclaration is KtPropertyAccessor) {
            ktDeclaration = ktDeclaration.property
        }

        if (ktDeclaration is KtConstructor<*>) {
            return ktDeclaration.getContainingClassOrObject().toLightClass()
        }

        val parent = ktDeclaration.parent

        if (parent is KtFile) {
            // top-level declaration
            return parent.findFacadeClass()
        } else if (parent is KtClassBody) {
            checkWithAttachment(parent.parent is KtClassOrObject, {
                "Bad parent: ${parent.parent?.javaClass}"
            }) {
                it.withPsiAttachment("parent", parent)
            }
            return (parent.parent as KtClassOrObject).toLightClass()
        }

        return null
    }

    private fun getWrappingClasses(declaration: KtDeclaration): Sequence<PsiClass> {
        val wrapperClass = getWrappingClass(declaration) ?: return emptySequence()
        val wrapperClassOrigin = (wrapperClass as KtLightClass).kotlinOrigin
        if (wrapperClassOrigin is KtObjectDeclaration && wrapperClassOrigin.isCompanion() && wrapperClass.parent is PsiClass) {
            return sequenceOf(wrapperClass, wrapperClass.parent as PsiClass)
        }
        return sequenceOf(wrapperClass)
    }

    fun canGenerateLightClass(declaration: KtDeclaration): Boolean {
        //noinspection unchecked
        return PsiTreeUtil.getParentOfType(declaration, KtFunction::class.java, KtProperty::class.java) == null
    }

    private fun KtDeclaration.isSpecialNameProvided(): Boolean {
        return annotationEntries.any { anno ->
            val target = if (JvmStandardClassIds.JVM_NAME.shortName() == anno.shortName) anno.useSiteTarget?.getAnnotationUseSiteTarget() else null
            target == AnnotationUseSiteTarget.PROPERTY_GETTER || target == AnnotationUseSiteTarget.PROPERTY_SETTER
        }
    }

    private fun <T> extractPropertyAccessors(
        ktDeclaration: T,
        specialGetter: PsiMethod?,
        specialSetter: PsiMethod?
    ): PropertyAccessorsPsiMethods where T : KtValVarKeywordOwner, T : KtNamedDeclaration {

        val accessorWrappers = when {
            ktDeclaration is KtProperty && noAdditionalAccessorsExpected(ktDeclaration, specialSetter, specialGetter) -> {
                emptySequence()
            }
            ktDeclaration.isSpecialNameProvided() -> {
                getPsiMethodWrappers(ktDeclaration)
            }
            else -> {
                val currentName = ktDeclaration.name
                val getterName = currentName?.let { JvmAbi.getterName(currentName) }
                val setterName = currentName?.let { JvmAbi.setterName(currentName) }
                getPsiMethodWrappers(ktDeclaration) { wrapper ->
                    val wrapperName = wrapper.name
                    currentName == null ||
                            currentName == wrapperName ||
                            wrapperName == getterName || isMangled(wrapperName, getterName!!) ||
                            wrapperName == setterName || isMangled(wrapperName, setterName!!) ||
                            DataClassResolver.isComponentLike(wrapperName)
                }
            }
        }

        val (setters, getters) = accessorWrappers.partition { it.isSetter }

        val allGetters = listOfNotNull(specialGetter) + getters.filterNot { it == specialGetter }
        val allSetters = listOfNotNull(specialSetter) + setters.filterNot { it == specialSetter }
        val backingField = getLightClassBackingField(ktDeclaration)
        val additionalAccessors = allGetters.drop(1) + allSetters.drop(1)
        return PropertyAccessorsPsiMethods(
            allGetters.firstOrNull(),
            allSetters.firstOrNull(),
            backingField,
            additionalAccessors
        )
    }

    private fun noAdditionalAccessorsExpected(
        ktDeclaration: KtProperty,
        specialSetter: PsiMethod?,
        specialGetter: PsiMethod?,
    ): Boolean {
        val containingClassOrObject = ktDeclaration.containingClassOrObject
        if ((containingClassOrObject as? KtObjectDeclaration)?.isCompanion() == true) {
            return false
        }
        return if (ktDeclaration.isVar) {
            specialSetter != null && specialGetter != null
        } else {
            specialGetter != null
        }
    }

    class PropertyAccessorsPsiMethods(
        val getter: PsiMethod?,
        val setter: PsiMethod?,
        val backingField: PsiField?,
        additionalAccessors: List<PsiMethod>
    ) : Iterable<PsiMethod> {
        private val allMethods: List<PsiMethod>
        val allDeclarations: List<PsiNamedElement>

        init {
            allMethods = arrayListOf()
            arrayOf(getter, setter).filterNotNullTo(allMethods)
            additionalAccessors.filterIsInstanceTo<PsiMethod, MutableList<PsiMethod>>(allMethods)

            allDeclarations = arrayListOf()
            arrayOf<PsiNamedElement?>(getter, setter, backingField).filterNotNullTo(allDeclarations)
            allDeclarations.addAll(additionalAccessors)
        }

        override fun iterator(): Iterator<PsiMethod> = allMethods.iterator()
    }
}

fun KtNamedDeclaration.getAccessorLightMethods(): LightClassUtil.PropertyAccessorsPsiMethods {
    return when (this) {
        is KtProperty -> LightClassUtil.getLightClassPropertyMethods(this)
        is KtParameter -> LightClassUtil.getLightClassPropertyMethods(this)
        else -> throw IllegalStateException("Unexpected property type: $this")
    }
}
