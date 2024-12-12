package org.jetbrains.kotlin.analysis.api.dumdum.index

import com.intellij.psi.stubs.ObjectStubSerializer
import com.intellij.psi.stubs.PsiFileStubImpl
import com.intellij.psi.stubs.StubSerializer
import com.intellij.psi.tree.IElementType

data class StubSerializersTable(val map: Map<String, ObjectStubSerializer<*, *>>) {
    fun getSerializer(serializerName: String): ObjectStubSerializer<*, *> =
        map[serializerName]!!
}

fun stubSerializersTable(): StubSerializersTable =
    StubSerializersTable(
        IElementType
            .enumerate { true }
            .filterIsInstance<StubSerializer<*>>()
            .associateByTo(hashMapOf(PsiFileStubImpl.TYPE.externalId to PsiFileStubImpl.TYPE)) { it.externalId }
    )
