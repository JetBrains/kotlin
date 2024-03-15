// Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.java.decompiler.util;

import java.lang.module.*;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;

import org.jetbrains.java.decompiler.main.DecompilerContext;
import org.jetbrains.java.decompiler.main.extern.IFernflowerLogger;
import org.jetbrains.java.decompiler.struct.StructContext;

public class ClasspathScanner {

    public static void addAllClasspath(StructContext ctx) {
      Set<String> found = new HashSet<String>();
      String[] props = { System.getProperty("java.class.path"), System.getProperty("sun.boot.class.path") };
      for (String prop : props) {
        if (prop == null)
          continue;

        for (final String path : prop.split(File.pathSeparator)) {
          File file = new File(path);
          if (found.contains(file.getAbsolutePath()))
            continue;

          if (file.exists() && (file.getName().endsWith(".class") || file.getName().endsWith(".jar"))) {
            DecompilerContext.getLogger().writeMessage("Adding File to context from classpath: " + file, IFernflowerLogger.Severity.INFO);
            ctx.addSpace(file, false);
            found.add(file.getAbsolutePath());
          }
        }
      }

      addAllModulePath(ctx);
    }

    private static void addAllModulePath(StructContext ctx) {
      for (ModuleReference module : ModuleFinder.ofSystem().findAll()) {
        String name = module.descriptor().name();
        try {
          ctx.addSpace(new ModuleContextSource(module), false);
        } catch (IOException e) {
          DecompilerContext.getLogger().writeMessage("Error loading module " + name, e);
        }
      }
    }

    static class ModuleContextSource extends ModuleBasedContextSource implements AutoCloseable {
      private final ModuleReader reader;

      public ModuleContextSource(final ModuleReference ref) throws IOException {
        super(ref.descriptor());
        this.reader = ref.open();
      }

      @Override
      public Stream<String> entryNames() throws IOException {
        return this.reader.list();
      }

      @Override
      public InputStream getInputStream(String resource) throws IOException {
        return this.reader.open(resource).orElse(null);
      }

      @Override
      public void close() throws Exception {
        this.reader.close();
      }
    }

}
