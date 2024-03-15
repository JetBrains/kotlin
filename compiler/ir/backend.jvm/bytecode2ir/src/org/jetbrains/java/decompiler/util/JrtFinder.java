package org.jetbrains.java.decompiler.util;

import org.jetbrains.java.decompiler.main.DecompilerContext;
import org.jetbrains.java.decompiler.main.extern.IContextSource;
import org.jetbrains.java.decompiler.main.extern.IFernflowerLogger;
import org.jetbrains.java.decompiler.struct.StructClass;
import org.jetbrains.java.decompiler.struct.StructContext;
import org.jetbrains.java.decompiler.struct.attr.StructGeneralAttribute;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.module.ModuleDescriptor;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class JrtFinder {
    public static final String CURRENT = "current";

    // https://openjdk.java.net/jeps/220 for runtime image structure and JRT filesystem

  public static void addRuntime(final StructContext ctx) {
    try {
      ctx.addSpace(new JavaRuntimeContextSource(null), false);
    } catch (final IOException ex) {
      DecompilerContext.getLogger().writeMessage("Failed to open current java runtime for inspection", ex);
    }
  }

  public static void addRuntime(final StructContext ctx, final File javaHome) {
    if (new File(javaHome, "lib/jrt-fs.jar").isFile()) {
      // Java 9+
      try {
        ctx.addSpace(new JavaRuntimeContextSource(javaHome), false);
      } catch (final IOException ex) {
        DecompilerContext.getLogger().writeMessage("Failed to open java runtime at " + javaHome, ex);
      }
      return;
    } else if (javaHome.exists()) {
      // legacy runtime, add all jars from the lib and jre/lib folders
      boolean anyAdded = false;
      final List<File> jrt = new ArrayList<>();
      Collections.addAll(jrt, new File(javaHome, "jre/lib").listFiles());
      Collections.addAll(jrt, new File(javaHome, "lib").listFiles());
      for (final File lib : jrt) {
        if (lib.isFile() && lib.getName().endsWith(".jar")) {
          ctx.addSpace(lib, false);
          anyAdded = true;
        }
      }
      if (anyAdded) return;
    }

    // does not exist
    DecompilerContext.getLogger().writeMessage("Unable to detect a java runtime at " + javaHome, IFernflowerLogger.Severity.ERROR);
  }

  static final class JavaRuntimeModuleContextSource extends ModuleBasedContextSource {
    private Path module;

    JavaRuntimeModuleContextSource(final ModuleDescriptor descriptor, final Path moduleRoot) {
      super(descriptor);
      this.module = moduleRoot;
    }

    @Override
    public InputStream getInputStream(String resource) throws IOException {
      return Files.newInputStream(this.module.resolve(resource));
    }

    @Override
    protected Stream<String> entryNames() throws IOException {
      try (final var dir = Files.walk(this.module)) {
        return dir.map(it -> this.module.relativize(it).toString()).collect(Collectors.toList()).stream();
      }
    }
  }

  static final class JavaRuntimeContextSource implements IContextSource, AutoCloseable {
    private final String identifier;
    private final FileSystem jrtFileSystem;

    public JavaRuntimeContextSource(final File javaHome) throws IOException {
      final var url = URI.create("jrt:/");
      if (javaHome == null) {
        this.identifier = "current";
        this.jrtFileSystem = FileSystems.newFileSystem(url, Map.of());
      } else {
        this.identifier = javaHome.getAbsolutePath();
        this.jrtFileSystem = FileSystems.newFileSystem(url, Map.of("java.home", javaHome.getAbsolutePath()));
      }
    }

    @Override
    public String getName() {
      return "Java runtime " + this.identifier;
    }

    @Override
    public Entries getEntries() {
      // One child source for every module in the runtime
      final List<IContextSource> children = new ArrayList<>();
      try {
      final List<Path> modules = Files.list(this.jrtFileSystem.getPath("modules")).collect(Collectors.toList());
      for (final Path module : modules) {
        ModuleDescriptor descriptor;
        try (final InputStream is = Files.newInputStream(module.resolve("module-info.class"))) {
          var clazz = StructClass.create(new DataInputFullStream(is.readAllBytes()), false);
          var moduleAttr = clazz.getAttribute(StructGeneralAttribute.ATTRIBUTE_MODULE);
          if (moduleAttr == null) continue;

          descriptor = moduleAttr.asDescriptor();
        } catch (final IOException ex) {
          continue;
        }
        children.add(new JavaRuntimeModuleContextSource(descriptor, module));
      }

        return new Entries(List.of(), List.of(), List.of(), children);
      } catch (final IOException ex) {
        DecompilerContext.getLogger().writeMessage("Failed to read modules from runtime " + this.identifier, ex);
        return Entries.EMPTY;
      }
    }

    @Override
    public InputStream getInputStream(String resource) throws IOException {
      return null; // all resources are part of a child provider
    }

    @Override
    public void close() throws IOException {
      this.jrtFileSystem.close();
    }
  }
}
