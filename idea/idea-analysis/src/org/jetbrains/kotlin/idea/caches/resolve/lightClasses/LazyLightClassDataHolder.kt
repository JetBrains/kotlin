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

import com.intellij.openapi.util.Key
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiMember
import com.intellij.psi.PsiMethod
import com.intellij.psi.impl.java.stubs.PsiJavaFileStub
import org.jetbrains.kotlin.asJava.LightClassBuilder
import org.jetbrains.kotlin.asJava.builder.*
import org.jetbrains.kotlin.asJava.classes.KtLightClass
import org.jetbrains.kotlin.asJava.classes.lazyPub
import org.jetbrains.kotlin.asJava.elements.KtLightField
import org.jetbrains.kotlin.asJava.elements.KtLightFieldImpl
import org.jetbrains.kotlin.asJava.elements.KtLightMethod
import org.jetbrains.kotlin.asJava.elements.KtLightMethodImpl
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.NotNullableUserDataProperty

typealias ExactLightClassContextProvider = () -> LightClassConstructionContext
typealias DummyLightClassContextProvider = (() -> LightClassConstructionContext?)?

sealed class LazyLightClassDataHolder(
        builder: LightClassBuilder,
        exactContextProvider: ExactLightClassContextProvider,
        dummyContextProvider: DummyLightClassContextProvider
) : LightClassDataHolder {

    private val exactResultLazyValue = lazyPub { builder(exactContextProvider()) }

    private val exactResult: LightClassBuilderResult by exactResultLazyValue

    private val lazyInexactResult by lazyPub {
        dummyContextProvider?.let { provider -> provider()?.let { context -> builder.invoke(context) } }
    }

    private val inexactResult: LightClassBuilderResult?
        get() = if (exactResultLazyValue.isInitialized()) null else lazyInexactResult

    override val javaFileStub get() = exactResult.stub
    override val extraDiagnostics get() = exactResult.diagnostics

    // for facade or defaultImpls
    override fun findData(findDelegate: (PsiJavaFileStub) -> PsiClass): LightClassData =
            LazyLightClassData { lightClassBuilderResult ->
                findDelegate(lightClassBuilderResult.stub)
            }

    class ForClass(
            builder: LightClassBuilder, exactContextProvider: ExactLightClassContextProvider, dummyContextProvider: DummyLightClassContextProvider
    ) : LazyLightClassDataHolder(builder, exactContextProvider, dummyContextProvider), LightClassDataHolder.ForClass {
        override fun findDataForClassOrObject(classOrObject: KtClassOrObject): LightClassData =
                LazyLightClassData { lightClassBuilderResult ->
                    lightClassBuilderResult.stub.findDelegate(classOrObject)
                }
    }

    class ForFacade(
            builder: LightClassBuilder, exactContextProvider: ExactLightClassContextProvider, dummyContextProvider: DummyLightClassContextProvider
    ) : LazyLightClassDataHolder(builder, exactContextProvider, dummyContextProvider), LightClassDataHolder.ForFacade

    class ForScript(
            builder: LightClassBuilder, exactContextProvider: ExactLightClassContextProvider, dummyContextProvider: DummyLightClassContextProvider
    ) : LazyLightClassDataHolder(builder, exactContextProvider, dummyContextProvider), LightClassDataHolder.ForScript

    private inner class LazyLightClassData(
            findDelegate: (LightClassBuilderResult) -> PsiClass
    ) : LightClassData {
        override val clsDelegate: PsiClass by lazyPub { findDelegate(exactResult) }

        private val dummyDelegate: PsiClass? by lazyPub { inexactResult?.let(findDelegate) }

        override fun getOwnFields(containingClass: KtLightClass): List<KtLightField> {
            if (dummyDelegate == null) return KtLightFieldImpl.fromClsFields(clsDelegate, containingClass)

            return dummyDelegate!!.fields.map { dummyField ->
                val fieldOrigin = KtLightFieldImpl.getOrigin(dummyField)!!

                val fieldName = dummyField.name!!
                KtLightFieldImpl.lazy(dummyField, fieldOrigin, containingClass) {
                    clsDelegate.findFieldByName(fieldName, false).assertMatches(dummyField, containingClass)
                }
            }
        }

        override fun getOwnMethods(containingClass: KtLightClass): List<KtLightMethod> {
            if (dummyDelegate == null) return KtLightMethodImpl.fromClsMethods(clsDelegate, containingClass)

            return dummyDelegate!!.methods.map { dummyMethod ->
                val methodOrigin = KtLightMethodImpl.getOrigin(dummyMethod)

                KtLightMethodImpl.lazy(dummyMethod, containingClass, methodOrigin) {
                    val dummyIndex = dummyMethod.memberIndex!!

                    val byMemberIndex: (PsiMethod) -> Boolean = { it.memberIndex == dummyIndex }

                    /* Searching all methods may be necessary in some cases where we failed to rollback optimization:
                            Overriding internal member that was final

                       Resulting light member is not consistent in this case, so this should happen only for erroneous code
                    */
                    val exactDelegateMethod = clsDelegate.findMethodsByName(dummyMethod.name, false).firstOrNull(byMemberIndex)
                                              ?: clsDelegate.methods.firstOrNull(byMemberIndex)
                    exactDelegateMethod.assertMatches(dummyMethod, containingClass)
                }
            }
        }
    }

    private fun <T : PsiMember> T?.assertMatches(dummyMember: T, containingClass: KtLightClass): T {
        if (this == null) throw LazyLightClassMemberMatchingError.NoMatch(dummyMember, containingClass)

        val parameterCountMatches = (this as? PsiMethod)?.parameterList?.parametersCount ?: 0 ==
                (dummyMember as? PsiMethod)?.parameterList?.parametersCount ?: 0
        if (this.memberIndex != dummyMember.memberIndex || !parameterCountMatches) {
            throw LazyLightClassMemberMatchingError.WrongMatch(this, dummyMember, containingClass)
        }

        return this
    }
}

private sealed class LazyLightClassMemberMatchingError(message: String, containingClass: KtLightClass)
    : AssertionError(message) {

    init {
        containingClass.kotlinOrigin?.hasLightClassMatchingErrors = true
    }

    class NoMatch(dummyMember: PsiMember, containingClass: KtLightClass) : LazyLightClassMemberMatchingError(
            "Couldn't match ${dummyMember.debugName} in $containingClass", containingClass
    )

    class WrongMatch(realMember: PsiMember, dummyMember: PsiMember, containingClass: KtLightClass) : LazyLightClassMemberMatchingError(
            "Matched ${dummyMember.debugName} to ${realMember.debugName} in $containingClass", containingClass
    )
}

private val PsiMember.debugName
    get() = "${this::class.java.simpleName}:${this.name} ${this.memberIndex}" + if (this is PsiMethod) " (with ${parameterList.parametersCount} parameters)" else ""

var KtClassOrObject.hasLightClassMatchingErrors: Boolean by NotNullableUserDataProperty(Key.create("LIGHT_CLASS_MATCHING_ERRORS"), false)
