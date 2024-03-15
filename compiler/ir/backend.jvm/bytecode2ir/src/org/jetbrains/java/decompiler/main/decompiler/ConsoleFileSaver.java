package org.jetbrains.java.decompiler.main.decompiler;

import org.jetbrains.java.decompiler.main.extern.IResultSaver;

import java.io.File;
import java.util.jar.Manifest;

// "Saves" a file to the standard out console
public final class ConsoleFileSaver implements IResultSaver {

  public ConsoleFileSaver(File unused) {

  }
  @Override
  public void saveFolder(String path) {

  }

  @Override
  public void copyFile(String source, String path, String entryName) {

  }

  @Override
  public void saveClassFile(String path, String qualifiedName, String entryName, String content, int[] mapping) {
    System.out.println("==== " + entryName + " ====");
    System.out.println(content);
  }

  @Override
  public void createArchive(String path, String archiveName, Manifest manifest) {

  }

  @Override
  public void saveDirEntry(String path, String archiveName, String entryName) {

  }

  @Override
  public void copyEntry(String source, String path, String archiveName, String entry) {

  }

  @Override
  public void saveClassEntry(String path, String archiveName, String qualifiedName, String entryName, String content) {
    System.out.println("==== " + entryName + " ====");
    System.out.println(content);
  }

  @Override
  public void closeArchive(String path, String archiveName) {

  }
}
