// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.psi.stubs;

import com.intellij.util.io.AbstractStringEnumerator;
import com.intellij.util.io.DataInputOutputUtil;
import com.intellij.util.io.IOUtil;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
//import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.ArrayList;
import java.util.function.UnaryOperator;

//@ApiStatus.Internal
final class FileLocalStringEnumerator implements AbstractStringEnumerator {
  @SuppressWarnings("SSBasedInspection")
  private final Object2IntOpenHashMap<String> myEnumerates;
  private final ArrayList<String> myStrings = new ArrayList<>();

  FileLocalStringEnumerator(boolean forSavingStub) {
    myEnumerates = forSavingStub ? new Object2IntOpenHashMap<>() : null;
  }

  @Override
  public int enumerate(@Nullable String value) {
    if (value == null) {
      return 0;
    }
    assert myEnumerates != null; // enumerate possible only when writing stub
    int i = myEnumerates.getInt(value);
    if (i == 0) {
      myEnumerates.put(value, i = myStrings.size() + 1);
      myStrings.add(value);
    }
    return i;
  }

  @Override
  public String valueOf(int idx) {
    if (idx == 0) return null;
    return myStrings.get(idx - 1);
  }

  void write(@NotNull DataOutput stream) throws IOException {
    assert myEnumerates != null;
    DataInputOutputUtil.writeINT(stream, myStrings.size());
    for(String s: myStrings) {
      IOUtil.writeUTF(stream, s);
    }
  }

  @Override
  public void markCorrupted() {
  }

  @Override
  public void close() {
  }

  @Override
  public boolean isDirty() {
    return false;
  }

  @Override
  public void force() {
  }

  void read(@NotNull DataInput stream, @NotNull UnaryOperator<String> mapping) throws IOException {
    int numberOfStrings = DataInputOutputUtil.readINT(stream);
    myStrings.ensureCapacity(myStrings.size() + numberOfStrings);
    for (int i = 0; i < numberOfStrings; i++) {
      myStrings.add(mapping.apply(IOUtil.readUTF(stream)));
    }
  }

  void read(@NotNull DataInput stream) throws IOException {
    int numberOfStrings = DataInputOutputUtil.readINT(stream);
    myStrings.ensureCapacity(myStrings.size() + numberOfStrings);
    for (int i = 0; i < numberOfStrings; i++) {
      myStrings.add(IOUtil.readUTF(stream));
    }
  }
}
