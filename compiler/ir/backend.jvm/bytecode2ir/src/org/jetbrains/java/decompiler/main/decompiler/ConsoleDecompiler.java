// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.java.decompiler.main.decompiler;

import org.jetbrains.java.decompiler.main.DecompilerContext;
import org.jetbrains.java.decompiler.main.Fernflower;
import org.jetbrains.java.decompiler.main.extern.IFernflowerLogger;
import org.jetbrains.java.decompiler.main.extern.IFernflowerPreferences;
import org.jetbrains.java.decompiler.main.extern.IResultSaver;
import org.jetbrains.java.decompiler.util.InterpreterUtil;
import org.jetbrains.java.decompiler.util.JrtFinder;
import org.jetbrains.java.decompiler.util.ZipFileCache;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.Function;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

public class ConsoleDecompiler implements /* IBytecodeProvider, */ IResultSaver, AutoCloseable {
  private static final Map<String, Object> CONSOLE_DEFAULT_OPTIONS = Map.of(
    IFernflowerPreferences.INCLUDE_JAVA_RUNTIME, JrtFinder.CURRENT
  );

  @SuppressWarnings("UseOfSystemOutOrSystemErr")
  public static void main(String[] args) {
    List<String> params = new ArrayList<String>();
    for (int x = 0; x < args.length; x++) {
      if (args[x].startsWith("-cfg")) {
        String path = null;
        if (args[x].startsWith("-cfg=")) {
          path = args[x].substring(5);
        }
        else if (args.length > x+1) {
          path = args[++x];
        }
        else {
          System.out.println("Must specify a file when using -cfg argument.");
          return;
        }
        Path file = Paths.get(path);
        if (!Files.exists(file)) {
          System.out.println("error: missing config '" + path + "'");
          return;
        }
        try (Stream<String> stream = Files.lines(file)) {
          stream.forEach(params::add);
        } catch (IOException e) {
          System.out.println("error: Failed to read config file '" + path + "'");
          throw new RuntimeException(e);
        }
      }
      else {
        params.add(args[x]);
      }
    }
    args = params.toArray(new String[params.size()]);

    if (Arrays.stream(args).anyMatch(arg -> arg.equals("-h") || arg.equals("--help") || arg.equals("-help"))) {
      ConsoleHelp.printHelp();
      return;
    }

    if (args.length < 1) {
      System.out.println(
        "Usage: java -jar vineflower.jar [-<option>=<value>]* [<source>]+ <destination>\n" +
        "Example: java -jar vineflower.jar -dgs=true c:\\my\\source\\ c:\\my.jar d:\\decompiled\\");
      return;
    }

    Map<String, Object> mapOptions = new HashMap<>(CONSOLE_DEFAULT_OPTIONS);
    List<File> sources = new ArrayList<>();
    List<File> libraries = new ArrayList<>();
    Set<String> whitelist = new HashSet<>();

    SaveType userSaveType = null;
    boolean isOption = true;
    int nonOption = 0;
    for (int i = 0; i < args.length; ++i) { // last parameter - destination
      String arg = args[i];

      switch (arg) {
        case "--file":
          if (userSaveType != null) {
            throw new RuntimeException("Multiple save types specified");
          }

          userSaveType = SaveType.FILE;
          continue;
        case "--folder":
          if (userSaveType != null) {
            throw new RuntimeException("Multiple save types specified");
          }

          userSaveType = SaveType.FOLDER;
          continue;
        case "--legacy-saving":
          if (userSaveType != null) {
            throw new RuntimeException("Multiple save types specified");
          }

          userSaveType = SaveType.LEGACY_CONSOLEDECOMPILER;
          continue;
      }

      if (isOption && arg.length() > 5 && arg.charAt(0) == '-' && arg.charAt(4) == '=') {
        String value = arg.substring(5);
        if ("true".equalsIgnoreCase(value)) {
          value = "1";
        }
        else if ("false".equalsIgnoreCase(value)) {
          value = "0";
        }

        mapOptions.put(arg.substring(1, 4), value);
      }
      else {
        nonOption++;
        // Don't process this, as it is the output
        if (nonOption > 1 && i == args.length - 1) {
          break;
        }

        isOption = false;

        if (arg.startsWith("-e=")) {
          addPath(libraries, arg.substring(3));
        }
        else if (arg.startsWith("-only=")) {
          whitelist.add(arg.substring(6));
        }
        else {
          addPath(sources, arg);
        }
      }
    }

    if (sources.isEmpty()) {
      System.out.println("error: no sources given");
      return;
    }

    SaveType saveType = SaveType.CONSOLE;

    File destination = new File("."); // Dummy value, when '.' we will be printing to console
    if (nonOption > 1) {
      String name = args[args.length - 1];

      saveType = SaveType.FOLDER;
      destination = new File(name);

      if (userSaveType == null) {
        if (destination.getName().contains(".zip") || destination.getName().contains(".jar")) {
          saveType = SaveType.FILE;

          if (destination.getParentFile() != null) {
            destination.getParentFile().mkdirs();
          }
        } else {
          destination.mkdirs();
        }
      } else {
        saveType = userSaveType;
      }
    }


    PrintStreamLogger logger = new PrintStreamLogger(System.out);
    ConsoleDecompiler decompiler = new ConsoleDecompiler(destination, mapOptions, logger, saveType);

    for (File library : libraries) {
      decompiler.addLibrary(library);
    }
    for (File source : sources) {
      decompiler.addSource(source);
    }
    for (String prefix : whitelist) {
      decompiler.addWhitelist(prefix);
    }

    decompiler.decompileContext();
  }

  @SuppressWarnings("UseOfSystemOutOrSystemErr")
  private static void addPath(List<? super File> list, String path) {
    File file = new File(path);
    if (file.exists()) {
      list.add(file);
    }
    else {
      System.out.println("warn: missing '" + path + "', ignored");
    }
  }

  // *******************************************************************
  // Implementation
  // *******************************************************************

  private final File root;
  private final Fernflower engine;
  private final Map<String, ZipOutputStream> mapArchiveStreams = new HashMap<>();
  private final Map<String, Set<String>> mapArchiveEntries = new HashMap<>();
  private final ZipFileCache openZips = new ZipFileCache();

  // Legacy support
  protected ConsoleDecompiler(File destination, Map<String, Object> options, IFernflowerLogger logger) {
    this(destination, options, logger, destination.isDirectory() ? SaveType.LEGACY_CONSOLEDECOMPILER : SaveType.FILE);
  }

  protected ConsoleDecompiler(File destination, Map<String, Object> options, IFernflowerLogger logger, SaveType saveType) {
    root = destination;
    engine = new Fernflower(saveType == SaveType.LEGACY_CONSOLEDECOMPILER ? this : saveType.getSaver().apply(destination), options, logger);
  }

  public void addSource(File source) {
    engine.addSource(source);
  }

  public void addLibrary(File library) {
    engine.addLibrary(library);
  }

  public void addWhitelist(String prefix) {
    engine.addWhitelist(prefix);
  }

  public void decompileContext() {
    try {
      engine.decompileContext();
    }
    finally {
      engine.clearContext();
    }
  }

  // *******************************************************************
  // Interface IBytecodeProvider
  // *******************************************************************

  // @Override
  public byte[] getBytecode(String externalPath, String internalPath) throws IOException { // UNUSED
    if (internalPath == null) {
      File file = new File(externalPath);
      return InterpreterUtil.getBytes(file);
    } else {
      final ZipFile archive = this.openZips.get(externalPath);
      ZipEntry entry = archive.getEntry(internalPath);
      if (entry == null) throw new IOException("Entry not found: " + internalPath);
      return InterpreterUtil.getBytes(archive, entry);
    }
  }

  // *******************************************************************
  // Interface IResultSaver
  // *******************************************************************

  private String getAbsolutePath(String path) {
    return new File(root, path).getAbsolutePath();
  }

  @Override
  public void saveFolder(String path) {
    File dir = new File(getAbsolutePath(path));
    if (!(dir.mkdirs() || dir.isDirectory())) {
      throw new RuntimeException("Cannot create directory " + dir);
    }
  }

  @Override
  public void copyFile(String source, String path, String entryName) {
    try {
      InterpreterUtil.copyFile(new File(source), new File(getAbsolutePath(path), entryName));
    }
    catch (IOException ex) {
      DecompilerContext.getLogger().writeMessage("Cannot copy " + source + " to " + entryName, ex);
    }
  }

  @Override
  public void saveClassFile(String path, String qualifiedName, String entryName, String content, int[] mapping) {
    File file = new File(getAbsolutePath(path), entryName);
    if (content != null) {
      try (Writer out = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8)) {
        out.write(content);
      } catch (IOException ex) {
        DecompilerContext.getLogger().writeMessage("Cannot write class file " + file, ex);
      }
    } else {
      DecompilerContext.getLogger().writeMessage("Attempted to write null class file to " + file, IFernflowerLogger.Severity.WARN);
    }
  }

  @Override
  public void createArchive(String path, String archiveName, Manifest manifest) {
    File file = new File(getAbsolutePath(path), archiveName);
    try {
      if (!(file.createNewFile() || file.isFile())) {
        throw new IOException("Cannot create file " + file);
      }

      FileOutputStream fileStream = new FileOutputStream(file);
      ZipOutputStream zipStream = manifest != null ? new JarOutputStream(fileStream, manifest) : new ZipOutputStream(fileStream);
      mapArchiveStreams.put(file.getPath(), zipStream);
    }
    catch (IOException ex) {
      DecompilerContext.getLogger().writeMessage("Cannot create archive " + file, ex);
    }
  }

  @Override
  public void saveDirEntry(String path, String archiveName, String entryName) {
    if (entryName.lastIndexOf('/') != entryName.length() - 1) {
      entryName += '/';
    }
    saveClassEntry(path, archiveName, null, entryName, null);
  }

  @Override
  public void copyEntry(String source, String path, String archiveName, String entryName) {
    String file = new File(getAbsolutePath(path), archiveName).getPath();

    if (!checkEntry(entryName, file)) {
      return;
    }

    try {
      ZipFile srcArchive = this.openZips.get(source);

      ZipEntry entry = srcArchive.getEntry(entryName);
      if (entry != null) {
        try (InputStream in = srcArchive.getInputStream(entry)) {
          ZipOutputStream out = mapArchiveStreams.get(file);
          out.putNextEntry(new ZipEntry(entryName));
          InterpreterUtil.copyStream(in, out);
        }
      }
    }
    catch (IOException ex) {
      String message = "Cannot copy entry " + entryName + " from " + source + " to " + file;
      DecompilerContext.getLogger().writeMessage(message, ex);
    }
  }

  @Override
  public void saveClassEntry(String path, String archiveName, String qualifiedName, String entryName, String content) {
    this.saveClassEntry(path, archiveName, qualifiedName, entryName, content, null);
  }

  @Override
  public void saveClassEntry(String path, String archiveName, String qualifiedName, String entryName, String content, int[] mapping) {
    String file = new File(getAbsolutePath(path), archiveName).getPath();

    if (!checkEntry(entryName, file)) {
      return;
    }

    try {
      ZipOutputStream out = mapArchiveStreams.get(file);
      ZipEntry entry = new ZipEntry(entryName);
      if (mapping != null && DecompilerContext.getOption(IFernflowerPreferences.DUMP_CODE_LINES)) {
        entry.setExtra(this.getCodeLineData(mapping));
      }
      out.putNextEntry(entry);
      if (content != null) {
        out.write(content.getBytes(StandardCharsets.UTF_8));
      }
    }
    catch (IOException ex) {
      String message = "Cannot write entry " + entryName + " to " + file;
      DecompilerContext.getLogger().writeMessage(message, ex);
    }
  }

  private boolean checkEntry(String entryName, String file) {
    Set<String> set = mapArchiveEntries.computeIfAbsent(file, k -> new HashSet<>());

    boolean added = set.add(entryName);
    if (!added) {
      String message = "Zip entry " + entryName + " already exists in " + file;
      DecompilerContext.getLogger().writeMessage(message, IFernflowerLogger.Severity.WARN);
    }
    return added;
  }

  @Override
  public void closeArchive(String path, String archiveName) {
    String file = new File(getAbsolutePath(path), archiveName).getPath();
    try {
      mapArchiveEntries.remove(file);
      mapArchiveStreams.remove(file).close();
    }
    catch (IOException ex) {
      DecompilerContext.getLogger().writeMessage("Cannot close " + file, IFernflowerLogger.Severity.WARN);
    }
  }

  @Override
  public void close() throws IOException {
    this.openZips.close();
  }

  public enum SaveType {
    LEGACY_CONSOLEDECOMPILER(null), // handled separately
    FOLDER(DirectoryResultSaver::new),
    FILE(SingleFileSaver::new),
    CONSOLE(ConsoleFileSaver::new);

    private final Function<File, IResultSaver> saver;

    SaveType(Function<File, IResultSaver> saver) {
      this.saver = saver;
    }

    public Function<File, IResultSaver> getSaver() {
      return saver;
    }
  }
}
