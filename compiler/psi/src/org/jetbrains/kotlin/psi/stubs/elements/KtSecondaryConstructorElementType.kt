/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package org.jetbrains.kotlin.psi.stubs.elements

import com.intellij.psi.stubs.StubElement
import com.intellij.util.io.StringRef
import org.jetbrains.kotlin.psi.KtSecondaryConstructor
import org.jetbrains.kotlin.psi.stubs.KotlinConstructorStub
import org.jetbrains.kotlin.psi.stubs.impl.KotlinConstructorStubImpl

class KtSecondaryConstructorElementType(debugName: String) :
    KtConstructorElementType<KtSecondaryConstructor>(debugName, KtSecondaryConstructor::class.java, KotlinConstructorStub::class.java) {
    override fun newStub(
        parentStub: StubElement<*>,
        nameRef: StringRef?,
        hasBody: Boolean,
        isDelegatedCallToThis: Boolean,
        isExplicitDelegationCall: Boolean,
    ): KotlinConstructorStub<KtSecondaryConstructor> {
        return KotlinConstructorStubImpl(
            parentStub, KtStubElementTypes.SECONDARY_CONSTRUCTOR, nameRef, hasBody, isDelegatedCallToThis, isExplicitDelegationCall
        )
    }

    override fun isDelegatedCallToThis(constructor: KtSecondaryConstructor) = constructor.getDelegationCallOrNull()?.isCallToThis ?: true

    override fun isExplicitDelegationCall(constructor: KtSecondaryConstructor) = constructor.getDelegationCallOrNull()?.isImplicit == false
}