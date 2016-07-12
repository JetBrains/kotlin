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

import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.util.Comparing
import com.intellij.psi.*
import com.intellij.psi.impl.java.stubs.PsiClassStub
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.stubs.PsiFileStub
import com.intellij.psi.stubs.StubElement
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.SmartList
import org.jetbrains.kotlin.fileClasses.javaFileFacadeFqName
import org.jetbrains.kotlin.load.java.JvmAbi
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getNonStrictParentOfType

object LightClassUtil {

    fun findClass(fqn: FqName, stub: StubElement<*>): PsiClass? {
        if (stub is PsiClassStub<*> && Comparing.equal(fqn.asString(), stub.qualifiedName)) {
            return stub.getPsi()
        }

        if (stub is PsiClassStub<*> || stub is PsiFileStub<*>) {
            for (child in stub.childrenStubs) {
                val answer = findClass(fqn, child)
                if (answer != null) return answer
            }
        }

        return null
    }/*package*/

    fun getLightClassAccessorMethod(accessor: KtPropertyAccessor): PsiMethod? =
            getLightClassAccessorMethods(accessor).firstOrNull()

    fun getLightClassAccessorMethods(accessor: KtPropertyAccessor): List<PsiMethod> {
        val property = accessor.getNonStrictParentOfType<KtProperty>() ?: return emptyList()
        val wrappers = getPsiMethodWrappers(property, true)
        return wrappers.filter { wrapper -> (accessor.isGetter && !JvmAbi.isSetterName(wrapper.name)) ||
                                            (accessor.isSetter && JvmAbi.isSetterName(wrapper.name)) }
    }

    fun getLightFieldForCompanionObject(companionObject: KtClassOrObject): PsiField? {
        val outerPsiClass = getWrappingClass(companionObject)
        if (outerPsiClass != null) {
            for (fieldOfParent in outerPsiClass.fields) {
                if ((fieldOfParent is KtLightElement<*, *>) && fieldOfParent.kotlinOrigin === companionObject) {
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
        return getPsiMethodWrappers(function, true)
    }

    private fun getPsiMethodWrapper(declaration: KtDeclaration): PsiMethod? {
        return getPsiMethodWrappers(declaration, false).firstOrNull()
    }

    private fun getPsiMethodWrappers(declaration: KtDeclaration, collectAll: Boolean): List<PsiMethod> {
        val psiClass = getWrappingClass(declaration) ?: return emptyList()

        val methods = SmartList<PsiMethod>()
        for (method in psiClass.methods.asList()) {
            try {
                if (method is KtLightMethod && method.kotlinOrigin === declaration) {
                    methods.add(method)
                    if (!collectAll) {
                        return methods
                    }
                }
            }
            catch (e: ProcessCanceledException) {
                throw e
            }
            catch (e: Throwable) {
                throw IllegalStateException(
                        "Error while wrapping declaration " + declaration.name + "Context\n:" + method, e)
            }
        }

        return methods
    }

    private fun getWrappingClass(declaration: KtDeclaration): PsiClass? {
        var declaration = declaration
        if (declaration is KtParameter) {
            val constructorClass = KtPsiUtil.getClassIfParameterIsProperty(declaration)
            if (constructorClass != null) {
                return constructorClass.toLightClass()
            }
        }

        if (declaration is KtPropertyAccessor) {
            declaration = declaration.property
        }

        if (declaration is KtConstructor<*>) {
            return declaration.getContainingClassOrObject().toLightClass()
        }

        val parent = declaration.parent

        if (parent is KtFile) {
            // top-level declaration
            val fqName = parent.javaFileFacadeFqName
            val project = declaration.project
            return JavaElementFinder.getInstance(project).findClass(fqName.asString(), GlobalSearchScope.allScope(project))
        }
        else if (parent is KtClassBody) {
            assert(parent.parent is KtClassOrObject)
            return (parent.parent as KtClassOrObject).toLightClass()
        }

        return null
    }

    fun canGenerateLightClass(declaration: KtDeclaration): Boolean {
        //noinspection unchecked
        return PsiTreeUtil.getParentOfType(declaration, KtFunction::class.java, KtProperty::class.java) == null
    }

    private fun extractPropertyAccessors(
            ktDeclaration: KtDeclaration,
            specialGetter: PsiMethod?, specialSetter: PsiMethod?): PropertyAccessorsPsiMethods {
        var getterWrapper = specialGetter
        var setterWrapper = specialSetter
        val additionalAccessors = arrayListOf<PsiMethod>()

        for (wrapper in getPsiMethodWrappers(ktDeclaration, true)) {
            if (JvmAbi.isSetterName(wrapper.name)) {
                if (setterWrapper == null || setterWrapper === specialSetter) {
                    setterWrapper = wrapper
                }
                else {
                    additionalAccessors.add(wrapper)
                }
            }
            else {
                if (getterWrapper == null || getterWrapper == specialGetter) {
                    getterWrapper = wrapper
                }
                else {
                    additionalAccessors.add(wrapper)
                }
            }
        }

        val backingField = getLightClassBackingField(ktDeclaration)
        return PropertyAccessorsPsiMethods(getterWrapper, setterWrapper, backingField, additionalAccessors)
    }

    fun buildLightTypeParameterList(
            owner: PsiTypeParameterListOwner,
            declaration: KtDeclaration): PsiTypeParameterList {
        val builder = KotlinLightTypeParameterListBuilder(owner.manager)
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
