/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi.stubs.factories

import com.intellij.lang.LighterAST
import com.intellij.lang.LighterASTNode
import com.intellij.psi.stubs.StubElement
import com.intellij.psi.stubs.StubInputStream
import com.intellij.psi.stubs.StubOutputStream
import com.intellij.util.io.StringRef
import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtSecondaryConstructor
import org.jetbrains.kotlin.psi.psiUtil.isLegacyContractPresentPsiCheck
import org.jetbrains.kotlin.psi.stubs.StubBuildingLexing.mayHaveLegacyContract
import org.jetbrains.kotlin.psi.stubs.StubUtils.deserializeKdocText
import org.jetbrains.kotlin.psi.stubs.StubUtils.serializeKdocText
import org.jetbrains.kotlin.psi.stubs.impl.KotlinSecondaryConstructorStubImpl
import org.jetbrains.kotlin.psi.stubs.light.anyDeclarationName
import org.jetbrains.kotlin.psi.stubs.light.containingClassOrObject
import org.jetbrains.kotlin.psi.stubs.light.firstChildOfType
import org.jetbrains.kotlin.psi.stubs.light.isLegacyContractPresent

internal object KtSecondaryConstructorStubSerializingElementFactory :
    KtStubSerializingElementFactory<KotlinSecondaryConstructorStubImpl, KtSecondaryConstructor>(
        type = KtNodeTypes.SECONDARY_CONSTRUCTOR,
    ) {

    override fun createPsi(
        stub: KotlinSecondaryConstructorStubImpl,
    ): KtSecondaryConstructor = KtSecondaryConstructor(stub)

    override fun createStub(
        psi: KtSecondaryConstructor,
        parentStub: StubElement<*>?,
    ): KotlinSecondaryConstructorStubImpl {
        val hasBody = psi.hasBody()
        val delegationCall = psi.getDelegationCallOrNull()
        val isDelegatedCallToThis = delegationCall?.isCallToThis ?: true
        val isExplicitDelegationCall = delegationCall?.isImplicit == false
        val mayHaveContract = psi.mayHaveLegacyContract() && psi.isLegacyContractPresentPsiCheck()
        return KotlinSecondaryConstructorStubImpl(
            parent = parentStub,
            containingClassName = StringRef.fromString(psi.name),
            hasBody = hasBody,
            isDelegatedCallToThis = isDelegatedCallToThis,
            isExplicitDelegationCall = isExplicitDelegationCall,
            mayHaveContract = mayHaveContract,
            kdocText = null,
        )
    }

    override fun createStub(
        tree: LighterAST,
        node: LighterASTNode,
        parentStub: StubElement<*>,
    ): KotlinSecondaryConstructorStubImpl {
        val delegationCall = tree.firstChildOfType(node, KtNodeTypes.CONSTRUCTOR_DELEGATION_CALL)
        val delegationReference = delegationCall?.let { tree.firstChildOfType(it, KtNodeTypes.CONSTRUCTOR_DELEGATION_REFERENCE) }
        val isDelegationReferenceEmpty = delegationReference != null && tree.getChildren(delegationReference).isEmpty()

        return KotlinSecondaryConstructorStubImpl(
            parent = parentStub,
            containingClassName = StringRef.fromString(
                tree.containingClassOrObject(node)?.let { tree.anyDeclarationName(it) },
            ),
            hasBody = tree.firstChildOfType(node, KtNodeTypes.BLOCK) != null,
            // Mirrors `delegationCall?.isCallToThis ?: true` with `isCallToThis` requiring a callee with `this`
            isDelegatedCallToThis = delegationCall == null ||
                    (delegationReference != null && tree.firstChildOfType(delegationReference, KtTokens.THIS_KEYWORD) != null),
            // Mirrors `delegationCall?.isImplicit == false`, where `isImplicit` requires a present but empty callee;
            // a delegation call without any callee reference counts as explicit
            isExplicitDelegationCall = delegationCall != null && (delegationReference == null || !isDelegationReferenceEmpty),
            mayHaveContract = tree.isLegacyContractPresent(node),
            kdocText = null,
        )
    }

    override fun serialize(stub: KotlinSecondaryConstructorStubImpl, dataStream: StubOutputStream) {
        dataStream.writeName(stub.name)
        dataStream.writeBoolean(stub.hasBody)
        dataStream.writeBoolean(stub.isDelegatedCallToThis)
        dataStream.writeBoolean(stub.isExplicitDelegationCall)
        dataStream.writeBoolean(stub.mayHaveContract)
        dataStream.serializeKdocText(stub.kdocText)
    }

    override fun deserialize(
        dataStream: StubInputStream,
        parentStub: StubElement<*>?,
    ): KotlinSecondaryConstructorStubImpl {
        val name = dataStream.readName()
        val hasBody = dataStream.readBoolean()
        val isDelegatedCallToThis = dataStream.readBoolean()
        val isExplicitDelegationCall = dataStream.readBoolean()
        val mayHaveContract = dataStream.readBoolean()
        val kdocText = dataStream.deserializeKdocText()
        return KotlinSecondaryConstructorStubImpl(
            parent = parentStub,
            containingClassName = name,
            hasBody = hasBody,
            isDelegatedCallToThis = isDelegatedCallToThis,
            isExplicitDelegationCall = isExplicitDelegationCall,
            mayHaveContract = mayHaveContract,
            kdocText = kdocText,
        )
    }
}
