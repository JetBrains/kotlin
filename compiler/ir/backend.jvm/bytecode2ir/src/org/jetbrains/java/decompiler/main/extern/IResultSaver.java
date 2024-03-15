// Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.java.decompiler.main.extern;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.jar.Manifest;

public interface IResultSaver extends AutoCloseable {
  void saveFolder(String path);

  void copyFile(String source, String path, String entryName);

  void saveClassFile(String path, String qualifiedName, String entryName, String content, int[] mapping);

  void createArchive(String path, String archiveName, Manifest manifest);

  void saveDirEntry(String path, String archiveName, String entryName);

  void copyEntry(String source, String path, String archiveName, String entry);

  void saveClassEntry(String path, String archiveName, String qualifiedName, String entryName, String content);

  default void saveClassEntry(final String path, final String archiveName, final String qualifiedName, final String entryName, final String content, final int[] mapping) {
    this.saveClassEntry(path, archiveName, qualifiedName, entryName, content);
  }

  void closeArchive(String path, String archiveName);

  @Override
  default void close() throws IOException {}

  default byte[] getCodeLineData(int[] mappings) {
    if (mappings == null || mappings.length == 0) {
      return null;
    }
    ByteBuffer buf = ByteBuffer.allocate(5 + (mappings.length * 2));
    buf.order(ByteOrder.LITTLE_ENDIAN);
    // Zip Extra entry header, described in http://www.info-zip.org/doc/appnote-19970311-iz.zip
    buf.putShort((short)0x4646); // FF - ForgeFlower
    buf.putShort((short)((mappings.length * 2) + 1)); // Mapping data + our version marker
    buf.put((byte)1); // Version code, in case we want to change it in the future.
    for (int line : mappings) {
        buf.putShort((short)line);
    }
    return buf.array();
  }
}
