/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

package org.jetbrains.kotlin.idea.caches.resolve.lightClasses

import com.intellij.psi.PsiClass
import com.intellij.psi.PsiClassType
import com.intellij.psi.impl.java.stubs.PsiJavaFileStub
import org.jetbrains.kotlin.asJava.builder.*
import org.jetbrains.kotlin.asJava.classes.KtLightClass
import org.jetbrains.kotlin.asJava.elements.KtLightField
import org.jetbrains.kotlin.asJava.elements.KtLightFieldImpl
import org.jetbrains.kotlin.asJava.elements.KtLightMethod
import org.jetbrains.kotlin.asJava.elements.KtLightMethodImpl
import org.jetbrains.kotlin.psi.KtClassOrObject

typealias LightClassBuilder = (LightClassConstructionContext) -> LightClassBuilderResult
typealias LightClassContextProvider = () -> LightClassConstructionContext

class LazyLightClassDataHolder(
        builder: LightClassBuilder,
        exactContextProvider: LightClassContextProvider,
        dummyContextProvider: LightClassContextProvider?
) : LightClassDataHolder {

    private val exactResultLazyValue = lazy(LazyThreadSafetyMode.PUBLICATION) { builder(exactContextProvider()) }

    private val exactResult: LightClassBuilderResult by exactResultLazyValue

    private val lazyInexactResult by lazy(LazyThreadSafetyMode.PUBLICATION) {
        dummyContextProvider?.let { builder.invoke(it()) }
    }

    private val inexactResult: LightClassBuilderResult?
        get() = if (exactResultLazyValue.isInitialized()) null else lazyInexactResult

    override val javaFileStub get() = exactResult.stub
    override val extraDiagnostics get() = exactResult.diagnostics

    // for facade or defaultImpls
    override fun findData(findDelegate: (PsiJavaFileStub) -> PsiClass): LightClassData =
            LazyLightClassData(relyOnDummySupertypes = true) { lightClassBuilderResult ->
                findDelegate(lightClassBuilderResult.stub)
            }

    override fun findDataForClassOrObject(classOrObject: KtClassOrObject): LightClassData =
            LazyLightClassData(relyOnDummySupertypes = classOrObject.getSuperTypeList() == null) { lightClassBuilderResult ->
                lightClassBuilderResult.stub.findDelegate(classOrObject)
            }

    private inner class LazyLightClassData(
            private val relyOnDummySupertypes: Boolean,
            findDelegate: (LightClassBuilderResult) -> PsiClass
    ) : LightClassData {
        override val clsDelegate: PsiClass by lazy(LazyThreadSafetyMode.PUBLICATION) { findDelegate(exactResult) }

        private val dummyDelegate: PsiClass? by lazy(LazyThreadSafetyMode.PUBLICATION) { inexactResult?.let(findDelegate) }

        override fun getOwnFields(containingClass: KtLightClass): List<KtLightField> {
            if (dummyDelegate == null) return clsDelegate.fields.map { KtLightFieldImpl.fromClsField(it, containingClass) }

            return dummyDelegate!!.fields.map { dummyField ->
                val memberOrigin = ClsWrapperStubPsiFactory.getMemberOrigin(dummyField)!!
                val fieldName = dummyField.name!!
                KtLightFieldImpl.lazy(dummyField, memberOrigin, containingClass) {
                    clsDelegate.findFieldByName(fieldName, false)!!.apply {
                        assert(this.memberIndex!! == dummyField.memberIndex!!)
                    }
                }
            }
        }

        override fun getOwnMethods(containingClass: KtLightClass): List<KtLightMethod> {
            if (dummyDelegate == null) return clsDelegate.methods.map { KtLightMethodImpl.fromClsMethod(it, containingClass) }

            return dummyDelegate!!.methods.map { dummyMethod ->
                val methodName = dummyMethod.name
                KtLightMethodImpl.lazy(dummyMethod, containingClass, ClsWrapperStubPsiFactory.getMemberOrigin(dummyMethod)) {
                    val dummyIndex = dummyMethod.memberIndex!!
                    clsDelegate.findMethodsByName(methodName, false).filter {
                        delegateCandidate -> delegateCandidate.memberIndex == dummyIndex
                    }.single().apply {
                        assert(this.parameterList.parametersCount == dummyMethod.parameterList.parametersCount)
                    }
                }
            }
        }

        override val supertypes: Array<PsiClassType>
            get() = if (relyOnDummySupertypes && dummyDelegate != null) dummyDelegate!!.superTypes else clsDelegate.superTypes

    }
}
