/*
 * Copyright 2010-2016 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.asJava

import com.intellij.psi.*
import com.intellij.psi.impl.java.stubs.PsiClassStub
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.stubs.PsiFileStub
import com.intellij.psi.stubs.StubElement
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.asJava.classes.KtLightClass
import org.jetbrains.kotlin.asJava.elements.*
import org.jetbrains.kotlin.asJava.finder.JavaElementFinder
import org.jetbrains.kotlin.fileClasses.javaFileFacadeFqName
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

    private fun getPsiMethodWrapper(declaration: KtDeclaration): PsiMethod? {
        return getPsiMethodWrappers(declaration).firstOrNull()
    }

    private fun getPsiMethodWrappers(declaration: KtDeclaration): Sequence<KtLightMethod> =
            getWrappingClasses(declaration).flatMap { it.methods.asSequence() }
                    .filterIsInstance<KtLightMethod>()
                    .filter { it.kotlinOrigin === declaration }

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
            return findFileFacade(parent)
        } else if (parent is KtClassBody) {
            checkWithAttachment(parent.parent is KtClassOrObject, {
                "Bad parent: ${parent.parent?.javaClass}"
            }) {
                it.withAttachment("parent", parent.text)
            }
            return (parent.parent as KtClassOrObject).toLightClass()
        }

        return null
    }

    private fun findFileFacade(ktFile: KtFile): PsiClass? {
        val fqName = ktFile.javaFileFacadeFqName
        val project = ktFile.project
        val classesWithMatchingFqName = JavaElementFinder.getInstance(project).findClasses(fqName.asString(), GlobalSearchScope.allScope(project))
        return classesWithMatchingFqName.singleOrNull() ?:
               classesWithMatchingFqName.find {
                   it.containingFile?.virtualFile == ktFile.virtualFile
               }
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
            specialGetter: PsiMethod?, specialSetter: PsiMethod?): PropertyAccessorsPsiMethods {

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

    fun buildLightTypeParameterList(
            owner: PsiTypeParameterListOwner,
            declaration: KtDeclaration): PsiTypeParameterList {
        val builder = KotlinLightTypeParameterListBuilder(owner)
        if (declaration is KtTypeParameterListOwner) {
            val parameters = declaration.typeParameters
            for (i in parameters.indices) {
                val jetTypeParameter = parameters.get(i)
                val name = jetTypeParameter.name
                val safeName = name ?: "__no_name__"
                builder.addParameter(KtLightTypeParameter(owner, i, safeName))
            }
        }
        return builder
    }

    class PropertyAccessorsPsiMethods(val getter: PsiMethod?,
                                             val setter: PsiMethod?,
                                             val backingField: PsiField?,
                                             additionalAccessors: List<PsiMethod>) : Iterable<PsiMethod> {
        private val allMethods: List<PsiMethod>
        val allDeclarations: List<PsiNamedElement>

        init {
            allMethods = arrayListOf<PsiMethod>()
            arrayOf(getter, setter).filterNotNullTo(allMethods)
            additionalAccessors.filterIsInstanceTo<PsiMethod, MutableList<PsiMethod>>(allMethods)

            allDeclarations = arrayListOf<PsiNamedElement>()
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
        else -> throw IllegalStateException("Unexpected property type: ${this}")
    }
}
