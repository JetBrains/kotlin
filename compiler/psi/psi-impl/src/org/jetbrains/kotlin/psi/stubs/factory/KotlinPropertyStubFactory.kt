/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi.stubs.factory

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.*
import com.intellij.util.io.StringRef
import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.psiUtil.safeFqNameForLazyResolve
import org.jetbrains.kotlin.psi.stubs.StubUtils.deserializeKdocText
import org.jetbrains.kotlin.psi.stubs.StubUtils.readNullableBoolean
import org.jetbrains.kotlin.psi.stubs.StubUtils.serializeKdocText
import org.jetbrains.kotlin.psi.stubs.StubUtils.writeNullableBoolean
import org.jetbrains.kotlin.psi.stubs.elements.KtFileElementType
import org.jetbrains.kotlin.psi.stubs.elements.KtStubElementTypes
import org.jetbrains.kotlin.psi.stubs.elements.StubIndexService
import org.jetbrains.kotlin.psi.stubs.impl.KotlinPropertyStubImpl
import org.jetbrains.kotlin.psi.stubs.impl.KotlinStubOrigin
import org.jetbrains.kotlin.psi.stubs.impl.deserializeConstantValue
import org.jetbrains.kotlin.psi.stubs.impl.serializeConstantValue

@Suppress("UnstableApiUsage") // KT-78356: the platform stub-decoupling API is still @ApiStatus.Experimental
internal object KotlinPropertyStubFactory : StubElementFactory<KotlinPropertyStubImpl, KtProperty> {
    /**
     * We want to build stubs for all non-local properties to make them indexable
     */
    override fun shouldCreateStub(node: ASTNode): Boolean {
        val parentNode = node.treeParent
        val parentElementType = parentNode.elementType

        // Simple check for non-local properties inside classes and files
        if (parentElementType == KtStubElementTypes.CLASS_BODY || parentElementType == KtFileElementType) {
            return true
        }

        // Simple check for local and non-local properties inside blocks
        if (parentElementType == KtNodeTypes.BLOCK) {
            val grandparentElementType = parentNode.treeParent.elementType
            if (grandparentElementType == KtStubElementTypes.SCRIPT) {
                return true
            }

            if (grandparentElementType == KtStubElementTypes.FUNCTION || grandparentElementType == KtStubElementTypes.PROPERTY_ACCESSOR) {
                return false
            }
        }

        // Fallback for psi-based check
        return !(node.psi as KtProperty).isLocal
    }

    override fun createStub(psi: KtProperty, parentStub: StubElement<out PsiElement>?): KotlinPropertyStubImpl {
        assert(!psi.isLocal) {
            "Should not store local property: ${psi.text}, parent ${psi.parent?.text ?: "<no parent>"}"
        }

        return KotlinPropertyStubImpl(
            parentStub,
            StringRef.fromString(psi.name),
            psi.isVar,
            psi.isTopLevel,
            psi.hasDelegate(),
            psi.hasDelegateExpression(),
            psi.hasInitializer(),
            psi.receiverTypeReference != null,
            psi.typeReference != null,
            psi.safeFqNameForLazyResolve(),
            /* constantInitializer = */ null,
            /* origin = */ null,
            /* hasBackingField = */ null,
            /* kdocText = */ null,
        )
    }

    override fun createPsi(stub: KotlinPropertyStubImpl): KtProperty = KtProperty(stub)
}

internal object KotlinPropertyStubSerializer : StubSerializer<KotlinPropertyStubImpl> {
    override fun getExternalId(): String = "kotlin.PROPERTY"

    override fun serialize(stub: KotlinPropertyStubImpl, dataStream: StubOutputStream) {
        dataStream.writeName(stub.name)
        dataStream.writeBoolean(stub.isVar)
        dataStream.writeBoolean(stub.isTopLevel)
        dataStream.writeBoolean(stub.hasDelegate)
        dataStream.writeBoolean(stub.hasDelegateExpression)
        dataStream.writeBoolean(stub.hasInitializer)
        dataStream.writeBoolean(stub.isExtension)
        dataStream.writeBoolean(stub.hasReturnTypeRef)

        val fqName = stub.fqName
        dataStream.writeName(fqName?.asString())

        serializeConstantValue(stub.constantInitializer, dataStream)

        KotlinStubOrigin.serialize(stub.origin, dataStream)

        dataStream.writeNullableBoolean(stub.hasBackingField)
        dataStream.serializeKdocText(stub.kdocText)
    }

    override fun deserialize(dataStream: StubInputStream, parentStub: StubElement<*>?): KotlinPropertyStubImpl {
        val name = dataStream.readName()
        val isVar = dataStream.readBoolean()
        val isTopLevel = dataStream.readBoolean()
        val hasDelegate = dataStream.readBoolean()
        val hasDelegateExpression = dataStream.readBoolean()
        val hasInitializer = dataStream.readBoolean()
        val hasReceiverTypeRef = dataStream.readBoolean()
        val hasReturnTypeRef = dataStream.readBoolean()

        val fqNameAsString = dataStream.readName()
        val fqName = fqNameAsString?.let { FqName(it.toString()) }

        val constantInitializer = deserializeConstantValue(dataStream)
        val stubOrigin = KotlinStubOrigin.deserialize(dataStream)
        val hasBackingField = dataStream.readNullableBoolean()
        val kdocText = dataStream.deserializeKdocText()
        return KotlinPropertyStubImpl(
            parentStub,
            name,
            isVar,
            isTopLevel,
            hasDelegate,
            hasDelegateExpression,
            hasInitializer,
            hasReceiverTypeRef,
            hasReturnTypeRef,
            fqName,
            constantInitializer,
            stubOrigin,
            hasBackingField,
            kdocText,
        )
    }

    override fun indexStub(stub: KotlinPropertyStubImpl, sink: IndexSink) {
        StubIndexService.getInstance().indexProperty(stub, sink)
    }
}
