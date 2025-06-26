/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package org.jetbrains.kotlin.psi.stubs.elements

import com.intellij.psi.stubs.StubElement
import com.intellij.util.io.StringRef
import org.jetbrains.kotlin.psi.KtPrimaryConstructor
import org.jetbrains.kotlin.psi.stubs.KotlinConstructorStub
import org.jetbrains.kotlin.psi.stubs.impl.KotlinConstructorStubImpl

class KtPrimaryConstructorElementType(debugName: String) :
    KtConstructorElementType<KtPrimaryConstructor>(debugName, KtPrimaryConstructor::class.java, KotlinConstructorStub::class.java) {
    override fun newStub(
        parentStub: StubElement<*>,
        nameRef: StringRef?,
        hasBody: Boolean,
        isDelegatedCallToThis: Boolean,
        isExplicitDelegationCall: Boolean,
        mayHaveContract: Boolean,
    ): KotlinConstructorStub<KtPrimaryConstructor> = KotlinConstructorStubImpl(
        parent = parentStub,
        elementType = KtStubElementTypes.PRIMARY_CONSTRUCTOR,
        containingClassName = nameRef,
        hasBody = hasBody,
        isDelegatedCallToThis = isDelegatedCallToThis,
        isExplicitDelegationCall = isExplicitDelegationCall,
        mayHaveContract = mayHaveContract,
    )

    override fun isDelegatedCallToThis(constructor: KtPrimaryConstructor) = false
    override fun isExplicitDelegationCall(constructor: KtPrimaryConstructor) = false
}