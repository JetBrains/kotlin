// Copyright 2000-2022 JetBrains s.r.o. and ForgeFlower contributors Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.java.decompiler.struct;

import org.jetbrains.java.decompiler.main.DecompilerContext;
import org.jetbrains.java.decompiler.main.decompiler.ConsoleDecompiler;
import org.jetbrains.java.decompiler.main.extern.IBytecodeProvider;
import org.jetbrains.java.decompiler.main.extern.IContextSource;
import org.jetbrains.java.decompiler.main.extern.IFernflowerLogger;
import org.jetbrains.java.decompiler.main.extern.IResultSaver;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;

public class DirectoryContextSource implements IContextSource {
  @SuppressWarnings("deprecation")
  private final IBytecodeProvider legacyProvider;
  private final File baseDirectory;

  @SuppressWarnings("deprecation")
  public DirectoryContextSource(final IBytecodeProvider legacyProvider, final File baseDirectory) {
    this.legacyProvider = legacyProvider;
    this.baseDirectory = baseDirectory;
  }

  @Override
  public String getName() {
    return "directory " + this.baseDirectory.getAbsolutePath();
  }

  @Override
  public Entries getEntries() {
    final List<Entry> classes = new ArrayList<>();
    final List<String> directories = new ArrayList<>();
    final List<Entry> others = new ArrayList<>();
    final List<IContextSource> jarChildren = new ArrayList<>();
    this.collectEntries(this.baseDirectory.getAbsolutePath(), this.baseDirectory, classes, directories, others, jarChildren);
    return new Entries(classes, directories, others, jarChildren);
  }

  void collectEntries(
    final String base,
    final File current,
    final List<Entry> classes,
    final List<String> directories,
    final List<Entry> others,
    final List<IContextSource> jarChildren
  ) {
    final String relativePath = relativize(base, current);
    if (current.isDirectory()) {
      directories.add(relativePath);
      final File[] children = current.listFiles();
      for (final File child : children) {
        collectEntries(base, child, classes, directories, others, jarChildren);
      }
    } else {
      if (relativePath.endsWith(CLASS_SUFFIX)) {
        classes.add(sanitize(relativePath.substring(0, relativePath.length() - CLASS_SUFFIX.length())));
      } else if (relativePath.endsWith(".jar") || relativePath.endsWith(".zip")) {
        final String relativeTo = current.getParentFile().getAbsolutePath().substring(base.length());
        try {
          jarChildren.add(new JarContextSource(this.legacyProvider, current, relativeTo));
        } catch (final IOException ex) {
          final String message = "Invalid archive " + current;
          DecompilerContext.getLogger().writeMessage(message, IFernflowerLogger.Severity.ERROR, ex);
          throw new UncheckedIOException(message, ex);
        }
      } else {
        others.add(sanitize(relativePath));
      }
    }
  }

  private String relativize(final String base, final File current) {
    final String relativePath = current.getAbsolutePath().substring(base.length());
    return relativePath.startsWith("/") || relativePath.startsWith("\\") ? relativePath.substring(1) : relativePath;
  }

  private Entry sanitize(final String path) {
    return Entry.atBase(path.replace(File.separatorChar, '/'));
  }

  @Override
  @SuppressWarnings("deprecation")
  public InputStream getInputStream(String resource) throws IOException {
    final File targetFile = new File(this.baseDirectory, resource);
    if (this.legacyProvider != null) {
      return new ByteArrayInputStream(this.legacyProvider.getBytecode(targetFile.getAbsolutePath(), null));
    } else {
      return new FileInputStream(targetFile);
    }
  }

  @Override
  public IOutputSink createOutputSink(IResultSaver saver) {
    final File base = this.baseDirectory;
    final String basePath = this.baseDirectory.getAbsolutePath();
    return new IOutputSink() {
      @Override
      public void begin() {
        // FIXME: ugly but needs to exist for folder->jar saving to work properly
        if (!(saver instanceof ConsoleDecompiler)) {
          saver.createArchive(basePath, "", null);
        }
        saver.saveFolder("");
      }

      @Override
      public void acceptOther(String path) {
        saver.copyFile(new File(base, path).getAbsolutePath(), "", path);
      }

      @Override
      public void acceptDirectory(String directory) {
        saver.saveFolder(directory);
      }

      @Override
      public void acceptClass(String qualifiedName, String fileName, String content, int[] mapping) {
        saver.saveClassFile("", qualifiedName, fileName, content, mapping);
      }

      @Override
      public void close() throws IOException {
      }
    };
  }
}
