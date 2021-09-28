/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.asJava

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
import org.jetbrains.kotlin.load.java.JvmAbi
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getNonStrictParentOfType
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
        val property = accessor.getNonStrictParentOfType<KtProperty>() ?: return emptyList()
        val wrappers = getPsiMethodWrappers(property)
        return wrappers.filter { wrapper ->
            (accessor.isGetter && !JvmAbi.isSetterName(wrapper.name)) ||
                    (accessor.isSetter && JvmAbi.isSetterName(wrapper.name))
        }.toList()
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
            }
        }

        for (field in psiClass.fields) {
            if (field is KtLightField && field.kotlinOrigin === declaration) {
                return field
            }
        }
        return null
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

    private fun getPsiMethodWrappers(declaration: KtDeclaration, name: String? = null): Sequence<KtLightMethod> =
        getWrappingClasses(declaration).flatMap { it.methods.asSequence() }
            .filterIsInstance<KtLightMethod>()
            .filter { name == null || name == it.name }
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

    private fun extractPropertyAccessors(
        ktDeclaration: KtDeclaration,
        specialGetter: PsiMethod?, specialSetter: PsiMethod?
    ): PropertyAccessorsPsiMethods {

        val (setters, getters) = getPsiMethodWrappers(ktDeclaration).partition { it.isSetter }

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
