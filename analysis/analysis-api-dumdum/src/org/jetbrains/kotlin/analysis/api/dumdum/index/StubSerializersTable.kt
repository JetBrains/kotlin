package org.jetbrains.kotlin.analysis.api.dumdum.index

import com.intellij.psi.impl.java.stubs.JavaStubElementTypes
import com.intellij.psi.stubs.ObjectStubSerializer
import com.intellij.psi.stubs.PsiFileStubImpl
import com.intellij.psi.stubs.StubSerializer
import com.intellij.psi.tree.IElementType
import org.jetbrains.kotlin.psi.stubs.elements.KtStubElementTypes

data class StubSerializersTable(val map: Map<String, ObjectStubSerializer<*, *>>) {
    fun getSerializer(serializerName: String): ObjectStubSerializer<*, *> =
        requireNotNull(map[serializerName]) {
            "no stub serializer for $serializerName\n $map"
        }
}

fun stubSerializersTable(): StubSerializersTable {
    KtStubElementTypes.CLASS;
    JavaStubElementTypes.CLASS;
    return StubSerializersTable(
        IElementType
            .enumerate { true }
            .filterIsInstance<StubSerializer<*>>()
            .associateByTo(
                destination = hashMapOf(PsiFileStubImpl.TYPE.externalId to PsiFileStubImpl.TYPE),
                keySelector = StubSerializer<*>::getExternalId
            )
    )
}
