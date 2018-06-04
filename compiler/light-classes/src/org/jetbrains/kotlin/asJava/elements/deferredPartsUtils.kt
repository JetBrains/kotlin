/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.asJava.elements

import com.intellij.openapi.util.Key
import com.intellij.openapi.util.UserDataHolder
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiTypeElement
import com.intellij.psi.impl.compiled.ClsRepositoryPsiElement
import com.intellij.psi.impl.compiled.ClsTypeElementImpl
import com.intellij.psi.impl.compiled.SignatureParsing
import com.intellij.psi.impl.compiled.StubBuildingVisitor
import org.jetbrains.kotlin.codegen.state.DeferredTypesTracker
import org.jetbrains.kotlin.utils.addToStdlib.safeAs
import java.text.StringCharacterIterator

@JvmField
val DEFERRED_TYPE_INFO = Key.create<Function0<DeferredTypesTracker.TypeInfo>>("DEFERRED_TYPE_INFO")

@JvmField
val DEFERRED_CONSTANT_INITIALIZER = Key.create<Function0<Any?>>("DEFERRED_CONSTANT_INITIALIZER")


internal fun KtLightElement<*, *>.computeChildTypeElement(
    clsDelegateTypeElement: PsiTypeElement?
): PsiTypeElement? {
    val delegateTypeElement = clsDelegateTypeElement as? ClsTypeElementImpl
    val canonicalText =
        getDeferredTypeInfoIfExists()?.jvmDescriptorOrGenericSignature?.let(::parseJvmDescriptorOrGenericSignature)
                ?: delegateTypeElement?.canonicalText
                ?: return null

    return ClsTypeElementImpl(this, canonicalText, /*ClsTypeElementImpl.VARIANCE_NONE */ 0.toChar())
}

private fun parseJvmDescriptorOrGenericSignature(value: String) =
    SignatureParsing.parseTypeString(StringCharacterIterator(value), StubBuildingVisitor.GUESSING_MAPPER)

internal fun KtLightElement<*, *>.getDeferredTypeInfoIfExists(): DeferredTypesTracker.TypeInfo? =
    clsDelegate.getUserDataFromStub(DEFERRED_TYPE_INFO)?.invoke()

internal fun <T> PsiElement.getUserDataFromStub(key: Key<T>) =
    safeAs<ClsRepositoryPsiElement<*>>()?.stub?.safeAs<UserDataHolder>()
        ?.getUserData(key)
