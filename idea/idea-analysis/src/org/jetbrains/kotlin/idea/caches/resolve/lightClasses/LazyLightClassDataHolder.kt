/*
 * Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.caches.resolve.lightClasses

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiMember
import com.intellij.psi.PsiMethod
import com.intellij.psi.impl.java.stubs.PsiJavaFileStub
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.intellij.psi.util.PsiModificationTracker
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
import org.jetbrains.kotlin.psi.debugText.getDebugText
import org.jetbrains.kotlin.utils.KotlinExceptionWithAttachments

typealias ExactLightClassContextProvider = () -> LightClassConstructionContext
typealias DummyLightClassContextProvider = (() -> LightClassConstructionContext?)?

sealed class LazyLightClassDataHolder(
        builder: LightClassBuilder,
        project: Project,
        exactContextProvider: ExactLightClassContextProvider,
        dummyContextProvider: DummyLightClassContextProvider,
        isLocal: Boolean = false
) : LightClassDataHolder {

    private val exactResultCachedValue =
        CachedValuesManager.getManager(project).createCachedValue({
            CachedValueProvider.Result.create(
                    builder(exactContextProvider()),
                    if (isLocal)
                        PsiModificationTracker.MODIFICATION_COUNT
                    else
                        PsiModificationTracker.OUT_OF_CODE_BLOCK_MODIFICATION_COUNT)
        }, false)

    private val lazyInexactStub by lazyPub {
        dummyContextProvider?.let { provider -> provider()?.let { context -> builder.invoke(context).stub } }
    }

    private val inexactStub: PsiJavaFileStub?
        get() = if (exactResultCachedValue.hasUpToDateValue()) null else lazyInexactStub

    override val javaFileStub get() = exactResultCachedValue.value.stub
    override val extraDiagnostics get() = exactResultCachedValue.value.diagnostics

    // for facade or defaultImpls
    override fun findData(findDelegate: (PsiJavaFileStub) -> PsiClass): LightClassData =
            LazyLightClassData { stub ->
                findDelegate(stub)
            }

    class ForClass(
            builder: LightClassBuilder, project: Project, isLocal: Boolean,
            exactContextProvider: ExactLightClassContextProvider, dummyContextProvider: DummyLightClassContextProvider
    ) : LazyLightClassDataHolder(builder, project, exactContextProvider, dummyContextProvider, isLocal), LightClassDataHolder.ForClass {
        override fun findDataForClassOrObject(classOrObject: KtClassOrObject): LightClassData =
                LazyLightClassData { stub ->
                    stub.findDelegate(classOrObject)
                }
    }

    class ForFacade(
            builder: LightClassBuilder, project: Project,
            exactContextProvider: ExactLightClassContextProvider, dummyContextProvider: DummyLightClassContextProvider
    ) : LazyLightClassDataHolder(builder, project, exactContextProvider, dummyContextProvider), LightClassDataHolder.ForFacade

    class ForScript(
            builder: LightClassBuilder, project: Project,
            exactContextProvider: ExactLightClassContextProvider, dummyContextProvider: DummyLightClassContextProvider
    ) : LazyLightClassDataHolder(builder, project, exactContextProvider, dummyContextProvider), LightClassDataHolder.ForScript

    private inner class LazyLightClassData(
            findDelegate: (PsiJavaFileStub) -> PsiClass
    ) : LightClassData {
        override val clsDelegate: PsiClass by lazyPub { findDelegate(javaFileStub) }

        private val dummyDelegate: PsiClass? by lazyPub { inexactStub?.let(findDelegate) }

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
    : KotlinExceptionWithAttachments(message) {

    init {
        containingClass.kotlinOrigin?.hasLightClassMatchingErrors = true
        withAttachment("class.kt", (containingClass.kotlinOrigin)?.getDebugText())
    }

    class NoMatch(dummyMember: PsiMember, containingClass: KtLightClass) : LazyLightClassMemberMatchingError(
            "Couldn't match ${dummyMember.debugName}", containingClass
    )

    class WrongMatch(realMember: PsiMember, dummyMember: PsiMember, containingClass: KtLightClass) : LazyLightClassMemberMatchingError(
            "Matched ${dummyMember.debugName} to ${realMember.debugName}", containingClass
    )
}

private val PsiMember.debugName
    get() = "${this::class.java.simpleName}:${this.name} ${this.memberIndex}" + if (this is PsiMethod) " (with ${parameterList.parametersCount} parameters)" else ""

var KtClassOrObject.hasLightClassMatchingErrors: Boolean by NotNullableUserDataProperty(Key.create("LIGHT_CLASS_MATCHING_ERRORS"), false)
