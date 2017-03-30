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

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiClassType
import com.intellij.psi.PsiMember
import com.intellij.psi.PsiMethod
import com.intellij.psi.impl.java.stubs.PsiJavaFileStub
import com.intellij.psi.search.EverythingGlobalScope
import com.intellij.psi.stubs.StubIndex
import org.jetbrains.kotlin.asJava.LightClassBuilder
import org.jetbrains.kotlin.asJava.builder.*
import org.jetbrains.kotlin.asJava.classes.KtLightClass
import org.jetbrains.kotlin.asJava.classes.lazyPub
import org.jetbrains.kotlin.asJava.elements.KtLightField
import org.jetbrains.kotlin.asJava.elements.KtLightFieldImpl
import org.jetbrains.kotlin.asJava.elements.KtLightMethod
import org.jetbrains.kotlin.asJava.elements.KtLightMethodImpl
import org.jetbrains.kotlin.idea.stubindex.KotlinOverridableInternalMembersShortNameIndex
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtCallableDeclaration
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtNamedDeclaration

typealias LightClassContextProvider = () -> LightClassConstructionContext

sealed class LazyLightClassDataHolder(
        builder: LightClassBuilder,
        exactContextProvider: LightClassContextProvider,
        dummyContextProvider: LightClassContextProvider?
) : LightClassDataHolder {

    private val exactResultLazyValue = lazyPub { builder(exactContextProvider()) }

    private val exactResult: LightClassBuilderResult by exactResultLazyValue

    private val lazyInexactResult by lazyPub {
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

    class ForClass(
            builder: LightClassBuilder, exactContextProvider: LightClassContextProvider, dummyContextProvider: LightClassContextProvider?
    ) : LazyLightClassDataHolder(builder, exactContextProvider, dummyContextProvider), LightClassDataHolder.ForClass {
        override fun findDataForClassOrObject(classOrObject: KtClassOrObject): LightClassData =
                LazyLightClassData(relyOnDummySupertypes = classOrObject.getSuperTypeList() == null) { lightClassBuilderResult ->
                    lightClassBuilderResult.stub.findDelegate(classOrObject)
                }
    }

    class ForFacade(
            builder: LightClassBuilder, exactContextProvider: LightClassContextProvider, dummyContextProvider: LightClassContextProvider?
    ) : LazyLightClassDataHolder(builder, exactContextProvider, dummyContextProvider), LightClassDataHolder.ForFacade

    private inner class LazyLightClassData(
            private val relyOnDummySupertypes: Boolean,
            findDelegate: (LightClassBuilderResult) -> PsiClass
    ) : LightClassData {
        override val clsDelegate: PsiClass by lazyPub { findDelegate(exactResult) }

        private val dummyDelegate: PsiClass? by lazyPub { inexactResult?.let(findDelegate) }

        override fun getOwnFields(containingClass: KtLightClass): List<KtLightField> {
            if (dummyDelegate == null) return KtLightFieldImpl.fromClsFields(clsDelegate, containingClass)

            return dummyDelegate!!.fields.map { dummyField ->
                val fieldOrigin = KtLightFieldImpl.getOrigin(dummyField)!!

                if (shouldRollbackOptimization(fieldOrigin)) {
                    return@getOwnFields KtLightFieldImpl.fromClsFields(clsDelegate, containingClass)
                }

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

                if (shouldRollbackOptimization(methodOrigin)) {
                    return@getOwnMethods KtLightMethodImpl.fromClsMethods(clsDelegate, containingClass)
                }

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

        override val supertypes: Array<PsiClassType>
            get() = if (relyOnDummySupertypes && dummyDelegate != null) dummyDelegate!!.superTypes else clsDelegate.superTypes

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

private fun shouldRollbackOptimization(origin: LightMemberOriginForDeclaration?): Boolean {
    val kotlinDeclaration = origin?.originalElement as? KtNamedDeclaration ?: return false

    if (!kotlinDeclaration.hasModifier(KtTokens.OVERRIDE_KEYWORD)) return false


    val possiblyOverridesInternalMember = kotlinDeclaration.name?.let { anyInternalMembersWithThisName(it, kotlinDeclaration.project) } ?: false
    return possiblyOverridesInternalMember
}


private fun anyInternalMembersWithThisName(name: String, project: Project): Boolean {
    var result = false
    StubIndex.getInstance().processElements(
            KotlinOverridableInternalMembersShortNameIndex.Instance.key, name, project,
            EverythingGlobalScope(project), KtCallableDeclaration::class.java
    ) {
        result = true
        false // stop processing at first matching result
    }
    return result
}

private sealed class LazyLightClassMemberMatchingError(message: String)
    : AssertionError(message) {

    class NoMatch(dummyMember: PsiMember, containingClass: KtLightClass) : LazyLightClassMemberMatchingError(
            "Couldn't match ${dummyMember.debugName} in $containingClass"
    )

    class WrongMatch(realMember: PsiMember, dummyMember: PsiMember, containingClass: KtLightClass) : LazyLightClassMemberMatchingError(
            "Matched ${dummyMember.debugName} to ${realMember.debugName} in $containingClass"
    )
}

private val PsiMember.debugName
    get() = "${this::class.simpleName}:${this.name} ${this.memberIndex}" + if (this is PsiMethod) " (with ${parameterList.parametersCount} parameters)" else ""
