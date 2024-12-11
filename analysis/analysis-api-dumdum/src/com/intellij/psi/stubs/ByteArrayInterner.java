// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.psi.stubs;

import com.intellij.openapi.util.io.BufferExposingByteArrayOutputStream;
import it.unimi.dsi.fastutil.Hash;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenCustomHashMap;
//import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;

//@ApiStatus.Internal
final class ByteArrayInterner {
  private static final Hash.Strategy<byte[]> BYTE_ARRAY_STRATEGY = new Hash.Strategy<byte[]>() {
    @Override
    public int hashCode(byte[] object) {
      return Arrays.hashCode(object);
    }

    @Override
    public boolean equals(byte[] o1, byte[] o2) {
      return Arrays.equals(o1, o2);
    }
  };
  private final Object2IntMap<byte[]> arrayToStart = new Object2IntOpenCustomHashMap<>(BYTE_ARRAY_STRATEGY);
  final BufferExposingByteArrayOutputStream joinedBuffer = new BufferExposingByteArrayOutputStream();

  int internBytes(byte[] bytes) {
    if (bytes.length == 0) return 0;

    int start = arrayToStart.getInt(bytes);
    if (start == 0) {
      start = joinedBuffer.size() + 1; // should be positive
      arrayToStart.put(bytes, start);
      joinedBuffer.write(bytes, 0, bytes.length);
    }
    return start;
  }
}
