/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(KtImplementationDetail::class)

package org.jetbrains.kotlin.psi.stubs.factories

import com.intellij.lang.ASTNode
import com.intellij.psi.stubs.IndexSink
import com.intellij.psi.stubs.StubElement
import com.intellij.psi.stubs.StubInputStream
import com.intellij.psi.stubs.StubOutputStream
import com.intellij.util.io.StringRef
import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtImplementationDetail
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.psiUtil.safeFqNameForLazyResolve
import org.jetbrains.kotlin.psi.stubs.StubUtils.deserializeKdocText
import org.jetbrains.kotlin.psi.stubs.StubUtils.readNullableBoolean
import org.jetbrains.kotlin.psi.stubs.StubUtils.serializeKdocText
import org.jetbrains.kotlin.psi.stubs.StubUtils.writeNullableBoolean
import org.jetbrains.kotlin.psi.stubs.elements.StubIndexService
import org.jetbrains.kotlin.psi.stubs.impl.KotlinPropertyStubImpl
import org.jetbrains.kotlin.psi.stubs.impl.KotlinStubOrigin
import org.jetbrains.kotlin.psi.stubs.impl.deserializeConstantValue
import org.jetbrains.kotlin.psi.stubs.impl.serializeConstantValue

internal object KtPropertyStubSerializingElementFactory :
    KtStubSerializingElementFactory<KotlinPropertyStubImpl, KtProperty>(
        type = KtNodeTypes.PROPERTY,
    ) {

    override fun createPsi(stub: KotlinPropertyStubImpl): KtProperty = KtProperty(stub)

    /**
     * We want to build stubs for all non-local properties to make them indexable
     */
    override fun shouldCreateStub(node: ASTNode): Boolean {
        val parentNode = node.treeParent
        val parentElementType = parentNode.elementType

        // Simple check for non-local properties inside classes and files
        if (parentElementType == KtNodeTypes.CLASS_BODY || parentElementType == KtNodeTypes.FILE) {
            return true
        }

        // Simple check for local and non-local properties inside blocks
        if (parentElementType == KtNodeTypes.BLOCK) {
            when (parentNode.treeParent.elementType) {
                KtNodeTypes.SCRIPT -> return true
                KtNodeTypes.FUN, KtNodeTypes.PROPERTY_ACCESSOR -> return false
            }
        }

        // Fallback for psi-based check
        return !(node.psi as KtProperty).isLocal
    }

    override fun createStub(psi: KtProperty, parentStub: StubElement<*>?): KotlinPropertyStubImpl {
        assert(!psi.isLocal) {
            "Should not store local property: ${psi.text}, parent ${psi.parent?.text ?: "<no parent>"}"
        }

        return KotlinPropertyStubImpl(
            parent = parentStub,
            name = StringRef.fromString(psi.name),
            isVar = psi.isVar,
            isTopLevel = psi.isTopLevel,
            hasDelegate = psi.hasDelegate(),
            hasDelegateExpression = psi.hasDelegateExpression(),
            hasInitializer = psi.hasInitializer(),
            isExtension = psi.receiverTypeReference != null,
            hasReturnTypeRef = psi.typeReference != null,
            fqName = psi.safeFqNameForLazyResolve(),
            constantInitializer = null,
            origin = null,
            hasBackingField = null,
            kdocText = null,
        )
    }

    override fun serialize(stub: KotlinPropertyStubImpl, dataStream: StubOutputStream) {
        dataStream.writeName(stub.name)
        dataStream.writeBoolean(stub.isVar)
        dataStream.writeBoolean(stub.isTopLevel)
        dataStream.writeBoolean(stub.hasDelegate)
        dataStream.writeBoolean(stub.hasDelegateExpression)
        dataStream.writeBoolean(stub.hasInitializer)
        dataStream.writeBoolean(stub.isExtension)
        dataStream.writeBoolean(stub.hasReturnTypeRef)
        dataStream.writeName(stub.fqName?.asString())

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
        val fqName = dataStream.readName()?.let { FqName(it.toString()) }
        val constantInitializer = deserializeConstantValue(dataStream)
        val origin = KotlinStubOrigin.deserialize(dataStream)
        val hasBackingField = dataStream.readNullableBoolean()
        val kdocText = dataStream.deserializeKdocText()
        return KotlinPropertyStubImpl(
            parent = parentStub,
            name = name,
            isVar = isVar,
            isTopLevel = isTopLevel,
            hasDelegate = hasDelegate,
            hasDelegateExpression = hasDelegateExpression,
            hasInitializer = hasInitializer,
            isExtension = hasReceiverTypeRef,
            hasReturnTypeRef = hasReturnTypeRef,
            fqName = fqName,
            constantInitializer = constantInitializer,
            origin = origin,
            hasBackingField = hasBackingField,
            kdocText = kdocText,
        )
    }

    override fun indexStub(stub: KotlinPropertyStubImpl, sink: IndexSink) {
        StubIndexService.getInstance().indexProperty(stub, sink)
    }
}
