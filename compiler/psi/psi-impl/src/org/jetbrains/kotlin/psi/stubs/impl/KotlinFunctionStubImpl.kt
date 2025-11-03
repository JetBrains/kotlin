/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi.stubs.impl

import com.intellij.psi.stubs.StubElement
import com.intellij.util.io.StringRef
import org.jetbrains.kotlin.contracts.description.KtContractDescriptionElement
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtImplementationDetail
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.stubs.KotlinFunctionStub
import org.jetbrains.kotlin.psi.stubs.elements.KtStubElementTypes

@OptIn(KtImplementationDetail::class)
class KotlinFunctionStubImpl(
    parent: StubElement<*>?,
    private val nameRef: StringRef?,
    override val isTopLevel: Boolean,
    override val fqName: FqName?,
    override val isExtension: Boolean,
    override val hasNoExpressionBody: Boolean,
    override val hasBody: Boolean,
    override val hasTypeParameterListBeforeFunctionName: Boolean,
    override val mayHaveContract: Boolean,
    val contract: List<KtContractDescriptionElement<KotlinTypeBean, Nothing?>>?,
    val origin: KotlinStubOrigin?,
) : KotlinStubBaseImpl<KtNamedFunction>(parent, KtStubElementTypes.FUNCTION), KotlinFunctionStub {
    init {
        if (isTopLevel && fqName == null) {
            throw IllegalArgumentException("fqName shouldn't be null for top level functions")
        }
    }

    override fun getName(): String? = StringRef.toString(nameRef)

    @KtImplementationDetail
    override fun copyInto(newParent: StubElement<*>?): KotlinFunctionStubImpl = KotlinFunctionStubImpl(
        parent = newParent,
        nameRef = nameRef,
        isTopLevel = isTopLevel,
        fqName = fqName,
        isExtension = isExtension,
        hasNoExpressionBody = hasNoExpressionBody,
        hasBody = hasBody,
        hasTypeParameterListBeforeFunctionName = hasTypeParameterListBeforeFunctionName,
        mayHaveContract = mayHaveContract,
        contract = contract,
        origin = origin,
    )
}
