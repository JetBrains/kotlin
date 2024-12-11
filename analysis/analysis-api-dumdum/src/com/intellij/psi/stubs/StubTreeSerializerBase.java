// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.psi.stubs;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.io.BufferExposingByteArrayOutputStream;
import com.intellij.psi.tree.IElementType;
import com.intellij.util.ArrayUtilRt;
import com.intellij.util.io.AbstractStringEnumerator;
import com.intellij.util.io.DataInputOutputUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.function.UnaryOperator;

abstract class StubTreeSerializerBase<SerializationState> {
  static final ThreadLocal<ObjectStubSerializer<?, ? extends Stub>> ourRootStubSerializer = new ThreadLocal<>();
  private static final boolean useStubStringInterner = Boolean.parseBoolean(System.getProperty("idea.use.stub.string.interner", "false"));


  StubTreeSerializerBase() {

  }

  @NotNull
  Stub deserialize(@NotNull InputStream stream) throws IOException, SerializerNotFoundException {
    FileLocalStringEnumerator storage = new FileLocalStringEnumerator(false);
    StubInputStream inputStream = new StubInputStream(stream, storage);

    @NotNull SerializationState state = readSerializationState(inputStream);

    storage.read(inputStream);

    final int stubFilesCount = DataInputOutputUtil.readINT(inputStream);
    if (stubFilesCount <= 0) {
      Logger.getInstance(getClass()).error("Incorrect stub files count during deserialization:" + stubFilesCount);
    }

    Stub baseStub = deserializeRoot(inputStream, storage, state);
    List<PsiFileStub<?>> stubs = new ArrayList<>(stubFilesCount);
    if (baseStub instanceof PsiFileStub<?>) {
      stubs.add((PsiFileStub<?>) baseStub);
    }
    for (int j = 1; j < stubFilesCount; j++) {
      Stub deserialize = deserializeRoot(inputStream, storage, state);
      if (deserialize instanceof PsiFileStub<?>) {
        stubs.add((PsiFileStub<?>) deserialize);
      }
      else {
        Logger.getInstance(getClass()).error("Stub root must be PsiFileStub for files with several stub roots");
      }
    }
    PsiFileStub<?>[] stubsArray = stubs.toArray(PsiFileStub.EMPTY_ARRAY);
    for (PsiFileStub<?> stub : stubsArray) {
      if (stub instanceof PsiFileStubImpl<?>) {
        ((PsiFileStubImpl<?>) stub).setStubRoots(stubsArray);
      }
    }
    return baseStub;
  }

  void serialize(@NotNull Stub rootStub, @NotNull OutputStream stream) throws IOException {
    BufferExposingByteArrayOutputStream out = new BufferExposingByteArrayOutputStream();
    FileLocalStringEnumerator storage = new FileLocalStringEnumerator(true);
    @NotNull SerializationState serializationState = createSerializationState();
    StubOutputStream stubOutputStream = new StubOutputStream(out, storage);
    boolean doDefaultSerialization = true;

    if (rootStub instanceof PsiFileStubImpl<?>) {
      PsiFileStubImpl<?> fileStub = (PsiFileStubImpl<?>) rootStub;
      PsiFileStub<?>[] roots = fileStub.getStubRoots();
      if (roots.length == 0) {
        Logger.getInstance(getClass()).error("Incorrect stub files count during serialization:" + rootStub + "," + rootStub.getStubType());
      }
      else {
        doDefaultSerialization = false;
        DataInputOutputUtil.writeINT(stubOutputStream, roots.length);
        for (PsiFileStub<?> root : roots) {
          serializeRoot(stubOutputStream, root, storage, serializationState);
        }
      }
    }

    if (doDefaultSerialization) {
      DataInputOutputUtil.writeINT(stubOutputStream, 1);
      serializeRoot(stubOutputStream, rootStub, storage, serializationState);
    }
    DataOutputStream resultStream = new DataOutputStream(stream);
    saveSerializationState(serializationState, resultStream);
    storage.write(resultStream);
    resultStream.write(out.getInternalBuffer(), 0, out.size());
  }

  protected abstract @NotNull SerializationState readSerializationState(@NotNull StubInputStream stream) throws IOException;


  protected abstract @NotNull SerializationState createSerializationState();


  protected abstract void saveSerializationState(@NotNull SerializationState state, @NotNull DataOutputStream stream)
          throws IOException;

  private Stub deserializeRoot(
          StubInputStream inputStream,
          FileLocalStringEnumerator storage,
          @NotNull SerializationState state
  ) throws IOException, SerializerNotFoundException {
    int serializedId = DataInputOutputUtil.readINT(inputStream);
    ObjectStubSerializer<?, ? extends Stub> serializer = getClassByIdLocal(serializedId, null, state);
    ourRootStubSerializer.set(serializer);
    try {
      Stub stub = serializer.deserialize(inputStream, null);
      if (stub instanceof StubBase<?>) {
        deserializeStubList((StubBase<?>) stub, serializer, inputStream, storage, state);
      }
      else {
        deserializeChildren(inputStream, stub, state);
      }
      return stub;
    }
    finally {
      ourRootStubSerializer.remove();
    }
  }

  protected abstract int writeSerializerId(
          @NotNull ObjectStubSerializer<Stub, Stub> serializer,
          @NotNull SerializationState state
  ) throws IOException;

  private void serializeSelf(
          Stub stub,
          @NotNull StubOutputStream stream,
          @NotNull SerializationState state
  ) throws IOException {
    if (((ObjectStubBase<?>) stub).isDangling()) {
      stream.writeByte(0);
    }
    writeSerializerId(stub, stream, state).serialize(stub, stream);
  }

  private @NotNull ObjectStubSerializer<Stub, Stub> writeSerializerId(
          Stub stub,
          @NotNull DataOutput stream,
          @NotNull SerializationState state
  )
          throws IOException {
    ObjectStubSerializer<Stub, Stub> serializer = StubSerializationUtil.getSerializer(stub);
    if (serializer == null) {
      throw new Error("No serializer was returned for " + stub);
    }
    int localId = writeSerializerId(serializer, state);
    DataInputOutputUtil.writeINT(stream, localId);
    return serializer;
  }

  private void serializeChildren(
          @NotNull Stub parent,
          @NotNull StubOutputStream stream,
          @NotNull SerializationState state
  ) throws IOException {
    final List<? extends Stub> children = parent.getChildrenStubs();
    DataInputOutputUtil.writeINT(stream, children.size());
    for (Stub child : children) {
      serializeSelf(child, stream, state);
      serializeChildren(child, stream, state);
    }
  }

  private void serializeRoot(
          StubOutputStream out,
          Stub root,
          AbstractStringEnumerator storage,
          @NotNull SerializationState state
  ) throws IOException {
    serializeSelf(root, out, state);
    if (root instanceof StubBase<?>) {
      StubBase<?> base = (StubBase<?>) root;
      StubList stubList = base.myStubList;
      if (root != stubList.get(0)) {
        throw new IllegalArgumentException("Serialization is supported only for root stubs");
      }
      serializeStubList(base, stubList, out, storage, state);
    }
    else {
      serializeChildren(root, out, state);
    }
  }

  private void deserializeStubList(
          StubBase<?> root,
          ObjectStubSerializer<?, ? extends Stub> rootType,
          StubInputStream inputStream,
          FileLocalStringEnumerator storage,
          @NotNull SerializationState state
  )
          throws IOException, SerializerNotFoundException {
    int stubCount = DataInputOutputUtil.readINT(inputStream);
    LazyStubList stubList = new LazyStubList(stubCount, root, rootType);

    MostlyUShortIntList parentsAndStarts = new MostlyUShortIntList(stubCount * 2);
    BitSet allStarts = new BitSet();

    new Object() {
      int currentIndex = 1;

      private void deserializeStub(int parentIndex) throws IOException, SerializerNotFoundException {
        int index = currentIndex;
        currentIndex++;

        int serializerId = DataInputOutputUtil.readINT(inputStream);
        ObjectStubSerializer<?, Stub> serializer = getClassByIdLocal(serializerId, null, state);

        int start = serializer instanceof EmptyStubSerializer ? 0 : DataInputOutputUtil.readINT(inputStream);

        allStarts.set(start);

        addStub(parentIndex, index, start, (IElementType) serializer);
        if (!serializer.isAlwaysLeaf(root)) {
          deserializeChildren(index);
        }
      }

      private void addStub(int parentIndex, int index, int start, IElementType type) {
        parentsAndStarts.add(parentIndex);
        parentsAndStarts.add(start);
        stubList.addLazyStub(type, index, parentIndex);
      }

      private void deserializeChildren(int parentIndex) throws IOException, SerializerNotFoundException {
        int childrenCount = DataInputOutputUtil.readINT(inputStream);
        stubList.prepareForChildren(parentIndex, childrenCount);
        for (int i = 0; i < childrenCount; i++) {
          deserializeStub(parentIndex);
        }
      }

      void deserializeRoot() throws IOException, SerializerNotFoundException {
        addStub(0, 0, 0, (IElementType) rootType);
        deserializeChildren(0);
      }
    }.deserializeRoot();
    byte[] serializedStubs = readByteArray(inputStream);
    stubList.setStubData(new LazyStubData(storage, parentsAndStarts, serializedStubs, allStarts));
  }

  private void serializeStubList(
          @NotNull StubBase<?> root,
          @NotNull StubList stubList,
          @NotNull DataOutput out,
          AbstractStringEnumerator storage,
          @NotNull SerializationState state
  ) throws IOException {
    if (!stubList.isChildrenLayoutOptimal()) {
      throw new IllegalArgumentException(
              "Manually assembled stubs should be normalized before serialization, consider wrapping them into StubTree");
    }

    DataInputOutputUtil.writeINT(out, stubList.size());
    DataInputOutputUtil.writeINT(out, stubList.getChildrenCount(0));

    BufferExposingByteArrayOutputStream tempBuffer = new BufferExposingByteArrayOutputStream();
    ByteArrayInterner interner = new ByteArrayInterner();

    for (int i = 1; i < stubList.size(); i++) {
      StubBase<?> stub = stubList.get(i);
      ObjectStubSerializer<Stub, Stub> serializer = writeSerializerId(stub, out, state);
      if (!(serializer instanceof EmptyStubSerializer)) {
        DataInputOutputUtil.writeINT(out, interner.internBytes(serializeStub(serializer, storage, stub, tempBuffer)));
      }
      int count = stubList.getChildrenCount(stub.id);
      if (!serializer.isAlwaysLeaf(root)) {
        DataInputOutputUtil.writeINT(out, count);
      }
      else {
        if (count != 0) {
          throw new IllegalStateException(
                  "Serializer reported that children are not possible, but they are present. Serializer = " +
                  serializer.getClass().getName() + "; Children count = " + count);
        }
      }
    }

    writeByteArray(out, interner.joinedBuffer.getInternalBuffer(), interner.joinedBuffer.size());
  }

  private static byte[] serializeStub(
          ObjectStubSerializer<Stub, Stub> serializer,
          AbstractStringEnumerator storage,
          StubBase<?> stub, BufferExposingByteArrayOutputStream tempBuffer
  ) throws IOException {
    tempBuffer.reset();
    StubOutputStream stubOut = new StubOutputStream(tempBuffer, storage);
    serializer.serialize(stub, stubOut);
    if (stub.isDangling()) {
      stubOut.writeByte(0);
    }
    return tempBuffer.size() == 0 ? ArrayUtilRt.EMPTY_BYTE_ARRAY : tempBuffer.toByteArray();
  }

  private byte[] readByteArray(StubInputStream inputStream) throws IOException {
    int length = DataInputOutputUtil.readINT(inputStream);
    if (length == 0) return ArrayUtilRt.EMPTY_BYTE_ARRAY;

    byte[] array = new byte[length];
    int read = inputStream.read(array);
    if (read != array.length) {
      Logger.getInstance(getClass()).error("Serialized array length mismatch");
    }
    return array;
  }

  private static void writeByteArray(DataOutput out, byte[] array, int len) throws IOException {
    DataInputOutputUtil.writeINT(out, len);
    out.write(array, 0, len);
  }

  protected abstract ObjectStubSerializer<?, Stub> getClassByIdLocal(
          int serializedId,
          @Nullable Stub parentStub,
          @NotNull SerializationState state
  ) throws SerializerNotFoundException;

  private void deserializeChildren(
          StubInputStream stream,
          Stub parent,
          @NotNull SerializationState state
  ) throws IOException, SerializerNotFoundException {
    int childCount = DataInputOutputUtil.readINT(stream);
    for (int i = 0; i < childCount; i++) {
      boolean dangling = false;
      int id = DataInputOutputUtil.readINT(stream);
      if (id == 0) {
        dangling = true;
        id = DataInputOutputUtil.readINT(stream);
      }

      Stub child = getClassByIdLocal(id, parent, state).deserialize(stream, parent);
      if (dangling) {
        ((ObjectStubBase<?>) child).markDangling();
      }
      deserializeChildren(stream, child, state);
    }
  }
}
