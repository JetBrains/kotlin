/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package com.intellij.openapi.vfs.impl.jar;

import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.impl.ZipHandler;
import com.intellij.util.containers.FactoryMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// This file was hijacked from com.jetbrains.intellij.platform:core-impl to backport the fix of IJPL-256842 from IntelliJ 263.
// The constructor took the root entry from a second `getEntryInfo("")` call. `ArchiveHandler` caches the entries map softly, so GC
// could collect it after the children were built, and the second call rebuilt the map with new `EntryInfo` instances. The root then
// came out without children, and the whole JAR looked empty. The constructor now takes the root from the map it built the children from.
// When the IntelliJ SDK is updated to 263 or later, this file can be dropped.

@SuppressWarnings({"unused", "UnstableApiUsage", "SuspiciousMethodCalls"})
final class CoreJarHandler extends ZipHandler {
  private final CoreJarFileSystem myFileSystem;
  private final VirtualFile myRoot;

  CoreJarHandler(@NotNull CoreJarFileSystem fileSystem, @NotNull String path) {
    super(path);
    myFileSystem = fileSystem;

    Map<EntryInfo, CoreJarVirtualFile> entries = new HashMap<>();

    Map<String, EntryInfo> entriesMap = getEntriesMap();
    Map<CoreJarVirtualFile, List<VirtualFile>> childrenMap = FactoryMap.create(key -> new ArrayList<>());
    for (EntryInfo info : entriesMap.values()) {
      CoreJarVirtualFile file = getOrCreateFile(info, entries);
      VirtualFile parent = file.getParent();
      if (parent != null) {
        childrenMap.get(parent).add(file);
      }
    }

    EntryInfo rootInfo = entriesMap.get("");
    myRoot = rootInfo != null ? getOrCreateFile(rootInfo, entries) : null;
    for (Map.Entry<CoreJarVirtualFile, List<VirtualFile>> entry : childrenMap.entrySet()) {
      List<VirtualFile> childList = entry.getValue();
      entry.getKey().setChildren(childList.toArray(VirtualFile.EMPTY_ARRAY));
    }
  }

  private CoreJarVirtualFile getOrCreateFile(EntryInfo info, Map<EntryInfo, CoreJarVirtualFile> entries) {
    CoreJarVirtualFile file = entries.get(info);
    if (file == null) {
      long length = info.isDirectory ? -1 : info.length;
      CoreJarVirtualFile parent = info.parent != null ? getOrCreateFile(info.parent, entries) : null;
      file = new CoreJarVirtualFile(this, info.shortName, length, info.timestamp, parent);
      entries.put(info, file);
    }
    return file;
  }

  @Nullable VirtualFile findFileByPath(@NotNull String pathInJar) {
    return myRoot != null ? myRoot.findFileByRelativePath(pathInJar) : null;
  }

  @NotNull CoreJarFileSystem getFileSystem() {
    return myFileSystem;
  }
}
