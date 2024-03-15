// Copyright 2000-2022 JetBrains s.r.o. and ForgeFlower contributors Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.java.decompiler.main.extern;

import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * A specific type of context unit.
 *
 * <p>Implementations do not need to cache the results of any provided methods.</p>
 */
public interface IContextSource {
  /**
   * The file extension for class files.
   */
  static String CLASS_SUFFIX = ".class";

  /**
   * Get a human-readable name to identify this context source.
   *
   * @return a human-readable name
   */
  String getName();

  /**
   * Get a listing of all entries in this context unit.
   *
   * @return the entries in this unit
   */
  Entries getEntries();

  /**
   * Get the full bytes for a class's contents.
   *
   * @param className the class name, with no trailing {@code /}
   * @return the bytes, or {@code null} if no class with that name is present
   * @throws IOException if an error is encountered while reading the class data
   */
  default byte[] getClassBytes(final String className) throws IOException {
    final InputStream is = this.getInputStream(className + CLASS_SUFFIX);
    if (is == null)
      return null;

    try (is) {
      return is.readAllBytes();
    }
  }

  /**
   * Get an input stream for a specific resource.
   *
   * This will return {@code null} if a directory is requested.
   *
   * @param resource the resource to request
   * @return an input stream
   * @throws IOException if an input stream could not be opened
   */
  default InputStream getInputStream(final Entry resource) throws IOException {
    return this.getInputStream(resource.path());
  }

  /**
   * Get an input stream for a specific resource.
   *
   * This will return {@code null} if a directory is requested.
   *
   * @param resource the resource to request
   * @return an input stream
   * @throws IOException if an input stream could not be opened
   */
  InputStream getInputStream(final String resource) throws IOException;

  /**
   * Create a sink that can write the decompiled output from this context element.
   *
   * <p>If this context source type does not support writing, return a null sink.</p>
   *
   * @param saver the source result saver for this decompiler, for delegation
   * @return the output sink, or null if unwritable
   */
  default /* @Nullable */ IOutputSink createOutputSink(final IResultSaver saver) {
    return null;
  }

  /**
   * A collector for output derived from this specific context entry.
   */
  interface IOutputSink extends AutoCloseable {
    /**
     * Begin this entry, performing any necessary setup work such as creating an archive
     */
    void begin();

    /**
     * Write a class to this entry
     *
     * @param qualifiedName the qualified name of the class
     * @param fileName the file name of the class, relative to its source
     * @param content the class text content
     * @param mapping a flat array of pairs of (input line number, output line number), null when -bsm=0
     */
    void acceptClass(final String qualifiedName, final String fileName, final String content, final int[] mapping);

    /**
     * Create a directory in this output location.
     *
     * @param directory the directory to create
     */
    void acceptDirectory(final String directory);

    /**
     * Accept other files, which should be copied directly through from the source.
     *
     * @param path the path
     */
    void acceptOther(final String path);

    @Override
    void close() throws IOException;
  }

  /**
   * All entries in the context unit.
   *
   * @param classes class names, with no {@value #CLASS_SUFFIX} suffix
   * @param directories directories, with no trailing {@code /}
   * @param others other entries
   * @param childContexts contexts discovered within this context
   */
  public static final class Entries {
    public static final Entries EMPTY = new Entries(List.of(), List.of(), List.of(), List.of());

    private final List<Entry> classes;
    private final List<String> directories;
    private final List<Entry> others;
    private final List<IContextSource> childContexts;

    public Entries(List<Entry> classes, List<String> directories, List<Entry> others) {
      this(classes, directories, others, List.of());
    }

    public Entries(List<Entry> classes, List<String> directories, List<Entry> others, List<IContextSource> childContexts) {
      // defensive copy
      this.classes = List.copyOf(classes);
      this.directories = List.copyOf(directories);
      this.others = List.copyOf(others);
      this.childContexts = List.copyOf(childContexts);
    }

    public List<Entry> classes() {
      return this.classes;
    }

    public List<String> directories() {
      return this.directories;
    }

    public List<Entry> others() {
      return this.others;
    }

    public List<IContextSource> childContexts() {
      return this.childContexts;
    }
  }

  /**
   * An entry in a context unit, which may be a multirelease variant.
   *
   * @param basePath the path of the entry, with any multirelease variant stripped
   * @param multirelease the multirelease target version, or {@value #BASE_VERSION} to indicate this entry is not part of a multirelease variant
   */
  public static final class Entry {
    public static final int BASE_VERSION = -1;
    private static final String MULTIRELEASE_PREFIX = "META-INF/versions/";

    private final String basePath;
    private final int multirelease;

    /**
     * Parse an entry from a raw jar path.
     *
     * @param path the path to parse
     * @return an entry, which may indicate a multirelease resource
     */
    public static Entry parse(final String path) {
      if (path.startsWith(MULTIRELEASE_PREFIX)) {
        final int nextSlash = path.indexOf('/', MULTIRELEASE_PREFIX.length());
        if (nextSlash == -1) return new Entry(path, BASE_VERSION);

        final String version = path.substring(MULTIRELEASE_PREFIX.length(), nextSlash);
        try {
          return new Entry(path.substring(nextSlash), Integer.parseInt(version));
        } catch (final NumberFormatException ex) {
          // unversioned
        }
      }

      return new Entry(path, BASE_VERSION);
    }

    /**
     * Create an entry at the base version, without attempting to parse any multirelease information.
     *
     * @param path the path to test
     * @return a new entry
     */
    public static Entry atBase(final String path) {
      return new Entry(path, BASE_VERSION);
    }

    public Entry(String basePath, int multirelease) {
      this.basePath = requireNonNull(basePath, "basePath");
      if (multirelease != -1 && multirelease < 9) {
        throw new IllegalArgumentException("A multirelease variant must target a Java runtime >= 9");
      }
      this.multirelease = multirelease;
    }

    public String basePath() {
      return this.basePath;
    }

    public int multirelease() {
      return this.multirelease;
    }

    public String path() {
      if (this.multirelease == BASE_VERSION) {
        return this.basePath();
      } else {
        return MULTIRELEASE_PREFIX + Integer.toString(this.multirelease) + '/' + this.basePath;
      }
    }


  }
}
