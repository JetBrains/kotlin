/*
 * Copyright 2000-2017 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.caches.lightClasses

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.util.Key
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiMember
import com.intellij.psi.PsiMethod
import com.intellij.psi.impl.java.stubs.PsiJavaFileStub
import org.jetbrains.kotlin.asJava.LightClassBuilder
import org.jetbrains.kotlin.asJava.builder.*
import org.jetbrains.kotlin.asJava.classes.KtLightClass
import org.jetbrains.kotlin.asJava.classes.lazySync
import org.jetbrains.kotlin.asJava.elements.KtLightField
import org.jetbrains.kotlin.asJava.elements.KtLightFieldImpl
import org.jetbrains.kotlin.asJava.elements.KtLightMethod
import org.jetbrains.kotlin.asJava.elements.KtLightMethodImpl
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.NotNullableUserDataProperty
import org.jetbrains.kotlin.psi.debugText.getDebugText
import org.jetbrains.kotlin.resolve.diagnostics.Diagnostics
import org.jetbrains.kotlin.storage.StorageManager

typealias ExactLightClassContextProvider = () -> LightClassConstructionContext
typealias DummyLightClassContextProvider = (() -> LightClassConstructionContext?)?
typealias DiagnosticsHolderProvider = () -> LazyLightClassDataHolder.DiagnosticsHolder

sealed class LazyLightClassDataHolder(
    private val builder: LightClassBuilder,
    private val exactContextProvider: ExactLightClassContextProvider,
    dummyContextProvider: DummyLightClassContextProvider,
    private val diagnosticsHolderProvider: DiagnosticsHolderProvider
) : LightClassDataHolder {

    class DiagnosticsHolder(storageManager: StorageManager) {
        private val cache = storageManager.createCacheWithNotNullValues<LazyLightClassDataHolder, Diagnostics>()

        fun getOrCompute(lazyLightClassDataHolder: LazyLightClassDataHolder, diagnostics: () -> Diagnostics) =
            cache.computeIfAbsent(lazyLightClassDataHolder, diagnostics)
    }

    private val _builderExactContextProvider: LightClassBuilderResult by lazySync { builder(exactContextProvider()) }

    private val exactResultLazyValue = lazySync { _builderExactContextProvider.stub }

    private val lazyInexactStub by lazySync {
        dummyContextProvider?.let { provider -> provider()?.let { context -> builder.invoke(context).stub } }
    }

    private val inexactStub: PsiJavaFileStub?
        get() = if (exactResultLazyValue.isInitialized()) null else lazyInexactStub

    override val javaFileStub by exactResultLazyValue

    override val extraDiagnostics: Diagnostics
        get() = diagnosticsHolderProvider().getOrCompute(this) {
            _builderExactContextProvider.diagnostics
                // Force lazy diagnostics computation because otherwise a lot of memory is retained by computation.
                // NB: Laziness here is not crucial anyway since somebody already has requested diagnostics and we hope one will use them
                .takeUnless { it.isEmpty() } ?: Diagnostics.EMPTY
        }

    // for facade or defaultImpls
    override fun findData(findDelegate: (PsiJavaFileStub) -> PsiClass): LightClassData =
        LazyLightClassData { stub ->
            findDelegate(stub)
        }

    class ForClass(
        builder: LightClassBuilder,
        exactContextProvider: ExactLightClassContextProvider,
        dummyContextProvider: DummyLightClassContextProvider,
        diagnosticsHolderProvider: DiagnosticsHolderProvider
    ) : LazyLightClassDataHolder(builder, exactContextProvider, dummyContextProvider, diagnosticsHolderProvider),
        LightClassDataHolder.ForClass {
        override fun findDataForClassOrObject(classOrObject: KtClassOrObject): LightClassData =
            LazyLightClassData { stub ->
                stub.findDelegate(classOrObject)
            }
    }

    class ForFacade(
        builder: LightClassBuilder,
        exactContextProvider: ExactLightClassContextProvider,
        dummyContextProvider: DummyLightClassContextProvider,
        diagnosticsHolderProvider: DiagnosticsHolderProvider
    ) : LazyLightClassDataHolder(builder, exactContextProvider, dummyContextProvider, diagnosticsHolderProvider),
        LightClassDataHolder.ForFacade

    class ForScript(
        builder: LightClassBuilder,
        exactContextProvider: ExactLightClassContextProvider,
        dummyContextProvider: DummyLightClassContextProvider,
        diagnosticsHolderProvider: DiagnosticsHolderProvider
    ) : LazyLightClassDataHolder(builder, exactContextProvider, dummyContextProvider, diagnosticsHolderProvider),
        LightClassDataHolder.ForScript

    private inner class LazyLightClassData(
        findDelegate: (PsiJavaFileStub) -> PsiClass
    ) : LightClassData {
        override val clsDelegate: PsiClass by lazySync { findDelegate(javaFileStub) }

        private val dummyDelegate: PsiClass? by lazySync { inexactStub?.let(findDelegate) }

        override fun getOwnFields(containingClass: KtLightClass): List<KtLightField> {
            if (dummyDelegate == null) return KtLightFieldImpl.fromClsFields(clsDelegate, containingClass)

            return dummyDelegate!!.fields.map { dummyField ->
                val fieldOrigin = KtLightFieldImpl.getOrigin(dummyField)

                val fieldName = dummyField.name
                KtLightFieldImpl.lazy(dummyField, fieldOrigin, containingClass) {
                    val findFieldByName = clsDelegate.findFieldByName(fieldName, false)
                    findFieldByName.checkMatches(dummyField, containingClass) ?:
                    // fallback in case of non-matched (like synthetic) fields
                    //
                    // it costs some performance and has to happen in rare and odd cases
                    KtLightFieldImpl.create(
                        KtLightFieldImpl.getOrigin(dummyField), dummyField, containingClass
                    )
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
                    val findMethodsByName = clsDelegate.findMethodsByName(dummyMethod.name, false)

                    val candidateDelegateMethod = findMethodsByName.firstOrNull(byMemberIndex)
                        ?: clsDelegate.methods.firstOrNull(byMemberIndex)

                    candidateDelegateMethod.checkMatches(dummyMethod, containingClass) ?:
                    // fallback if unable to find method for a dummy method (e.g. synthetic methods marked explicit or implicit) are
                    // not visible as own methods.
                    //
                    // it costs some performance and has to happen in rare and odd cases
                    KtLightMethodImpl.create(dummyMethod, KtLightMethodImpl.getOrigin(dummyMethod), containingClass)
                }
            }
        }
    }

    private fun <T : PsiMember> T?.checkMatches(dummyMember: T, containingClass: KtLightClass): T? {
        if (this == null) {
            logMismatch("Couldn't match ${dummyMember.debugName}", containingClass)
            return null
        }

        val parameterCountMatches = (this as? PsiMethod)?.parameterList?.parametersCount ?: 0 ==
                (dummyMember as? PsiMethod)?.parameterList?.parametersCount ?: 0
        if (this.memberIndex != dummyMember.memberIndex || !parameterCountMatches) {
            logMismatch("Wrongly matched ${dummyMember.debugName} to ${this.debugName}", containingClass)
            return null
        }

        return this
    }

    companion object {
        private val LOG = Logger.getInstance(LazyLightClassDataHolder::class.java)

        private fun logMismatch(message: String, containingClass: KtLightClass) {
            containingClass.kotlinOrigin?.hasLightClassMatchingErrors = true
            LOG.warn("$message, class.kt: ${(containingClass.kotlinOrigin)?.getDebugText()}")
        }
    }
}

private val PsiMember.debugName
    get() = "${this::class.java.simpleName}:${this.name} ${this.memberIndex}" + if (this is PsiMethod) " (with ${parameterList.parametersCount} parameters)" else ""

var KtClassOrObject.hasLightClassMatchingErrors: Boolean by NotNullableUserDataProperty(Key.create("LIGHT_CLASS_MATCHING_ERRORS"), false)
