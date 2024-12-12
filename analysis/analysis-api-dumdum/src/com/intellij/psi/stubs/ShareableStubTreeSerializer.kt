// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.psi.stubs

//import org.jetbrains.annotations.ApiStatus
//import org.jetbrains.annotations.ApiStatus.Internal
import org.jetbrains.kotlin.analysis.api.dumdum.index.StubSerializersTable
import java.io.*

//@Internal
class ShareableStubTreeSerializer(val stubSerializersTable: StubSerializersTable) : StubTreeSerializer {
  private val serializer = object : StubTreeSerializerBase<FileLocalStringEnumerator>() {
    override fun readSerializationState(stream: StubInputStream): FileLocalStringEnumerator {
      val enumerator = FileLocalStringEnumerator(false)
      enumerator.read(stream)
      return enumerator
    }

    override fun createSerializationState(): FileLocalStringEnumerator =
      FileLocalStringEnumerator(true)

    override fun saveSerializationState(state: FileLocalStringEnumerator, stream: DataOutputStream) =
      state.write(stream)

    override fun writeSerializerId(
      serializer: ObjectStubSerializer<Stub, Stub>,
      state: FileLocalStringEnumerator,
    ): Int =
      state.enumerate(serializer.externalId)

    override fun getClassByIdLocal(localId: Int, parentStub: Stub?, state: FileLocalStringEnumerator): ObjectStubSerializer<*, Stub> {
      val serializerName = state.valueOf(localId)
        ?: throw SerializerNotFoundException("Can't find serializer for local id $localId")
      @Suppress("UNCHECKED_CAST")
      return stubSerializersTable.getSerializer(serializerName) as ObjectStubSerializer<*, Stub>
    }
  }

  override fun serialize(rootStub: Stub, stream: OutputStream) {

    serializer.serialize(rootStub, stream)
  }

  @Throws(SerializerNotFoundException::class)
  override fun deserialize(stream: InputStream): Stub {

    return serializer.deserialize(stream)
  }

}