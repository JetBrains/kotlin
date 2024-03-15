package org.jetbrains.java.decompiler.main.decompiler;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import org.jetbrains.java.decompiler.main.DecompilerContext;
import org.jetbrains.java.decompiler.main.extern.IFernflowerLogger;
import org.jetbrains.java.decompiler.main.extern.IFernflowerPreferences;
import org.jetbrains.java.decompiler.main.extern.IResultSaver;
import org.jetbrains.java.decompiler.util.InterpreterUtil;
import org.jetbrains.java.decompiler.util.ZipFileCache;

public class SingleFileSaver implements IResultSaver, AutoCloseable {
  private final File target;
  private ZipOutputStream output;
  private Set<String> entries = new HashSet<>();
  private final ZipFileCache openZips = new ZipFileCache();

  public SingleFileSaver(File target) {
    this.target = target;
    if (target.isDirectory()) {
      throw new IllegalStateException("Trying to save " + target.getAbsolutePath() + " as a file but there's a directory there already!");
    }
  }

  @Override
  public void saveFolder(String path) {

  }

  @Override
  public void copyFile(String source, String path, String entryName) {
    if (!checkEntry(entryName))
      return;

    try {
      output.putNextEntry(new ZipEntry(entryName));
      InterpreterUtil.copyStream(new FileInputStream(source), output);
    } catch (IOException ex) {
      String message = "Cannot write entry " + entryName + " to " + target;
      DecompilerContext.getLogger().writeMessage(message, ex);
    }
  }

  @Override
  public void saveClassFile(String path, String qualifiedName, String entryName, String content, int[] mapping) {
    if (!checkEntry(qualifiedName + ".java"))
      return;

    try {
      output.putNextEntry(new ZipEntry(qualifiedName + ".java"));

      if (content != null) {
        output.write(content.getBytes(StandardCharsets.UTF_8));
      }
    } catch (IOException ex) {
      String message = "Cannot write entry " + entryName + " to " + target;
      DecompilerContext.getLogger().writeMessage(message, ex);
    }
  }

  @Override
  public void createArchive(String path, String archiveName, Manifest manifest) {
    if (output != null)
      throw new UnsupportedOperationException("Attempted to write multiple archives at the same time");
    try {
      FileOutputStream stream = new FileOutputStream(target);
      output = manifest != null ? new JarOutputStream(stream, manifest) : new ZipOutputStream(stream);
    } catch (IOException e) {
      DecompilerContext.getLogger().writeMessage("Cannot create archive " + target, e);
    }
  }

  @Override
  public void saveDirEntry(String path, String archiveName, String entryName) {
//    saveClassEntry(path, archiveName, null, entryName, null);
  }

  @Override
  public void copyEntry(String source, String path, String archiveName, String entryName) {
    if (!checkEntry(entryName))
      return;

    try {
      ZipFile srcArchive = this.openZips.get(source);
      ZipEntry entry = srcArchive.getEntry(entryName);
      if (entry != null) {
        try (InputStream in = srcArchive.getInputStream(entry)) {
          output.putNextEntry(new ZipEntry(entryName));
          InterpreterUtil.copyStream(in, output);
        }
      }
    }
    catch (IOException ex) {
      String message = "Cannot copy entry " + entryName + " from " + source + " to " + target;
      DecompilerContext.getLogger().writeMessage(message, ex);
    }
  }

  @Override
  public void saveClassEntry(String path, String archiveName, String qualifiedName, String entryName, String content) {
    this.saveClassEntry(path, archiveName, qualifiedName, entryName, content, null);
  }

  @Override
  public void saveClassEntry(String path, String archiveName, String qualifiedName, String entryName, String content, int[] mapping) {
    if (!checkEntry(entryName))
        return;

    try {
      ZipEntry entry = new ZipEntry(entryName);
      if (mapping != null && DecompilerContext.getOption(IFernflowerPreferences.DUMP_CODE_LINES))
        entry.setExtra(this.getCodeLineData(mapping));
      output.putNextEntry(entry);
      if (content != null)
          output.write(content.getBytes(StandardCharsets.UTF_8));
    }
    catch (IOException ex) {
      String message = "Cannot write entry " + entryName + " to " + target;
      DecompilerContext.getLogger().writeMessage(message, ex);
    }
  }

  @Override
  public void closeArchive(String path, String archiveName) {
    try {
      if (this.output != null) {
        output.close();
        entries.clear();
        output = null;
      }
    }
    catch (IOException ex) {
      DecompilerContext.getLogger().writeMessage("Cannot close " + target, IFernflowerLogger.Severity.WARN);
    }
  }

  private boolean checkEntry(String entryName) {
    boolean added = entries.add(entryName);
    if (!added) {
      String message = "Zip entry " + entryName + " already exists in " + target;
      DecompilerContext.getLogger().writeMessage(message, IFernflowerLogger.Severity.WARN);
    }
    return added;
  }

  @Override
  public void close() throws IOException {
    this.openZips.close();
  }
}
