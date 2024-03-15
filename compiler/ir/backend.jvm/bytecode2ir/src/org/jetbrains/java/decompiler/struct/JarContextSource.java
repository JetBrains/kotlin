// Copyright 2000-2022 JetBrains s.r.o. and ForgeFlower contributors Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.java.decompiler.struct;

import org.jetbrains.java.decompiler.main.DecompilerContext;
import org.jetbrains.java.decompiler.main.extern.IBytecodeProvider;
import org.jetbrains.java.decompiler.main.extern.IContextSource;
import org.jetbrains.java.decompiler.main.extern.IFernflowerLogger;
import org.jetbrains.java.decompiler.main.extern.IResultSaver;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static java.util.Objects.requireNonNull;

final class JarContextSource implements IContextSource, AutoCloseable {
  private static final String MANIFEST = "META-INF/MANIFEST.MF";

  @SuppressWarnings("deprecation")
  private final IBytecodeProvider legacyProvider;
  private final String relativePath; // used for nested contexts from DirectoryContextSource
  private final File jarFile;
  private final ZipFile file;
  private final boolean isZip;

  @SuppressWarnings("deprecation")
  JarContextSource(final IBytecodeProvider legacyProvider, final File archive) throws IOException {
    this(legacyProvider, archive, "");
  }

  @SuppressWarnings("deprecation")
  JarContextSource(final IBytecodeProvider legacyProvider, final File archive, final String relativePath) throws IOException {
    this.legacyProvider = legacyProvider;
    this.relativePath = relativePath;
    this.jarFile = requireNonNull(archive, "archive");
    this.file = new ZipFile(archive);
    this.isZip = this.jarFile.getName().endsWith("zip");
  }

  @Override
  public String getName() {
    return "archive " + this.jarFile.getAbsolutePath();
  }

  @Override
  public Entries getEntries() {
    final List<Entry> classes = new ArrayList<>();
    final Set<String> directories = new LinkedHashSet<>();
    final List<Entry> others = new ArrayList<>();

    Enumeration<? extends ZipEntry> entries = this.file.entries();
    while (entries.hasMoreElements()) {
      ZipEntry entry = entries.nextElement();

      String name = entry.getName();
      addDirectories(entry, directories);
      if (!entry.isDirectory()) {
        if (name.endsWith(CLASS_SUFFIX)) {
          classes.add(Entry.parse(name.substring(0, name.length() - CLASS_SUFFIX.length())));
        }
        else if (this.isZip || !name.equalsIgnoreCase(MANIFEST)) {
          others.add(Entry.parse(name));
        }
      }
    }
    return new Entries(classes, List.copyOf(directories), others, List.of());
  }

  private void addDirectories(final ZipEntry entry, final Set<String> directories) {
    final String name = entry.getName();
    int segmentIndex = name.indexOf('/');
    while (segmentIndex != -1) {
      directories.add(name.substring(0, segmentIndex));
      segmentIndex = name.indexOf('/', segmentIndex + 1);
    }

    if (entry.isDirectory()) {
      directories.add(name);
    }
  }

  @Override
  @SuppressWarnings("deprecation")
  public InputStream getInputStream(String resource) throws IOException {
    if (this.legacyProvider != null) {
      return new ByteArrayInputStream(this.legacyProvider.getBytecode(this.jarFile.getAbsolutePath(), resource));
    }

    final ZipEntry entry = this.file.getEntry(resource);
    return this.file.getInputStream(entry);
  }

  @Override
  public IOutputSink createOutputSink(IResultSaver saver) {
    final String archiveName = this.jarFile.getName();
    return new IOutputSink() {
      @Override
      public void begin() {
        final ZipEntry potentialManifest = file.getEntry(MANIFEST);
        Manifest manifest = null;
        if (potentialManifest != null) {
          try (final InputStream is = file.getInputStream(potentialManifest)) {
            manifest = new Manifest(is);
          } catch (final IOException ex) {
            DecompilerContext.getLogger().writeMessage("Failed to read manifest from " + file, IFernflowerLogger.Severity.ERROR, ex);
          }
        }

        saver.saveFolder(relativePath);
        saver.createArchive(relativePath, archiveName, manifest);
      }

      @Override
      public void acceptOther(String path) {
        saver.copyEntry(jarFile.getAbsolutePath(), relativePath, archiveName, path);
      }

      @Override
      public void acceptDirectory(String directory) {
        saver.saveDirEntry(relativePath, archiveName, directory);
      }

      @Override
      public void acceptClass(String qualifiedName, String fileName, String content, int[] mapping) {
        saver.saveClassEntry(relativePath, jarFile.getName(), qualifiedName, fileName, content, mapping);
      }

      @Override
      public void close() throws IOException {
        saver.closeArchive(relativePath, archiveName);
      }
    };
  }

  @Override
  public void close() throws IOException {
    this.file.close();
  }
}
