// Copyright 2000-2022 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.java.decompiler.util;

import org.jetbrains.java.decompiler.main.DecompilerContext;
import org.jetbrains.java.decompiler.main.extern.IContextSource;
import org.jetbrains.java.decompiler.main.extern.IFernflowerLogger;

import java.io.IOException;
import java.lang.module.ModuleDescriptor;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

abstract class ModuleBasedContextSource implements IContextSource {
  private final ModuleDescriptor ref;

  public ModuleBasedContextSource(final ModuleDescriptor ref) {
    this.ref = ref;
  }

  @Override
  public String getName() {
    return "module " + this.ref.toNameAndVersion();
  }

  protected abstract Stream<String> entryNames() throws IOException;

  @Override
  public Entries getEntries() {
    final List<Entry> classNames = new ArrayList<>();
    final List<String> directoryNames = new ArrayList<>();
    final List<Entry> otherEntries = new ArrayList<>();

    try {
      this.entryNames().forEach(name -> {
        if (name.endsWith("/")) {
          directoryNames.add(name.substring(0, name.length() - 1));
        } else if (name.endsWith(CLASS_SUFFIX)) {
          classNames.add(Entry.atBase(name.substring(0, name.length() - CLASS_SUFFIX.length())));
        } else {
          otherEntries.add(Entry.atBase(name));
        }
      });
    } catch (final IOException ex) {
      DecompilerContext.getLogger().writeMessage("Failed to list contents of " + this.getName(), IFernflowerLogger.Severity.ERROR, ex);
    }

    return new Entries(classNames, directoryNames, otherEntries);
  }
}