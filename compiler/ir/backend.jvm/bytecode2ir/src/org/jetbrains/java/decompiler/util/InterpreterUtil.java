// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.java.decompiler.util;

import java.io.*;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public final class InterpreterUtil {
  public static final boolean IS_WINDOWS = System.getProperty("os.name", "").startsWith("Windows");

  public static final int[] EMPTY_INT_ARRAY = new int[0];

  private static final int BUFFER_SIZE = 16 * 1024;

  public static void copyFile(File source, File target) throws IOException {
    try (FileInputStream in = new FileInputStream(source); FileOutputStream out = new FileOutputStream(target)) {
      copyStream(in, out);
    }
  }

  public static void copyStream(InputStream in, OutputStream out) throws IOException {
    in.transferTo(out);
  }

  public static byte[] getBytes(ZipFile archive, ZipEntry entry) throws IOException {
    try (InputStream stream = archive.getInputStream(entry)) {
      return readBytes(stream, (int)entry.getSize());
    }
  }

  public static byte[] getBytes(File file) throws IOException {
    try (FileInputStream stream = new FileInputStream(file)) {
      return readBytes(stream, (int)file.length());
    }
  }

  public static byte[] readBytes(InputStream stream, int length) throws IOException {
    byte[] bytes = stream.readNBytes(length);

    if (bytes.length < length) {
      throw new IOException("premature end of stream");
    }

    return bytes;
  }

  public static void discardBytes(InputStream stream, int length) throws IOException {
    if (stream.skip(length) != length) {
      throw new IOException("premature end of stream");
    }
  }

  public static boolean equalSets(Collection<?> c1, Collection<?> c2) {
    if (c1 == null) {
      return c2 == null;
    }
    else if (c2 == null) {
      return false;
    }

    if (c1.size() != c2.size()) {
      return false;
    }

    HashSet<Object> set = new HashSet<>(c1);
    set.removeAll(c2);
    return (set.size() == 0);
  }

  public static boolean equalObjects(Object first, Object second) {
    return first == null ? second == null : first.equals(second);
  }

  public static boolean equalLists(List<?> first, List<?> second) {
    if (first == null) {
      return second == null;
    }
    else if (second == null) {
      return false;
    }

    if (first.size() == second.size()) {
      for (int i = 0; i < first.size(); i++) {
        if (!equalObjects(first.get(i), second.get(i))) {
          return false;
        }
      }
      return true;
    }

    return false;
  }

  public static String makeUniqueKey(String name, String descriptor) {
    return name + ' ' + descriptor;
  }

  public static String makeUniqueKey(String name, String descriptor1, String descriptor2) {
    return name + ' ' + descriptor1 + ' ' + descriptor2;
  }
}