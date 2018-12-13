/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jetbrains.kotlin.utils;

import com.intellij.openapi.util.SystemInfo;
import com.intellij.openapi.util.io.FileFilters;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.io.FileUtilRt;
import com.intellij.openapi.util.registry.Registry;
import com.intellij.util.ArrayUtil;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.List;
import java.util.Set;

import static com.intellij.util.ObjectUtils.notNull;

//todo[Alefas]: copypaste from org.jetbrains.jps.model.java.impl.JavaSdkUtil
public class JavaSdkUtil {
  @NotNull
  public static List<File> getJdkClassesRoots(@NotNull File home, boolean isJre) {
    File[] jarDirs;
    if (SystemInfo.isMac && !home.getName().startsWith("mockJDK")) {
      File openJdkRtJar = new File(home, "jre/lib/rt.jar");
      if (openJdkRtJar.isFile()) {
        File libDir = new File(home, "lib");
        File classesDir = openJdkRtJar.getParentFile();
        File libExtDir = new File(openJdkRtJar.getParentFile(), "ext");
        File libEndorsedDir = new File(libDir, "endorsed");
        jarDirs = new File[]{libEndorsedDir, libDir, classesDir, libExtDir};
      }
      else {
        File libDir = new File(home, "lib");
        File classesDir = new File(home, "../Classes");
        File libExtDir = new File(libDir, "ext");
        File libEndorsedDir = new File(libDir, "endorsed");
        jarDirs = new File[]{libEndorsedDir, libDir, classesDir, libExtDir};
      }
    }
    else if (new File(home, "lib/jrt-fs.jar").exists()) {
      jarDirs = ArrayUtil.EMPTY_FILE_ARRAY;
    }
    else {
      File libDir = new File(home, isJre ? "lib" : "jre/lib");
      File libExtDir = new File(libDir, "ext");
      File libEndorsedDir = new File(libDir, "endorsed");
      jarDirs = new File[]{libEndorsedDir, libDir, libExtDir};
    }

    FileFilter jarFileFilter = FileFilters.filesWithExtension("jar");
    Set<String> pathFilter = ContainerUtil.newTroveSet(FileUtil.PATH_HASHING_STRATEGY);
    List<File> rootFiles = ContainerUtil.newArrayList();
    if (Registry.is("project.structure.add.tools.jar.to.new.jdk")) {
      File toolsJar = new File(home, "lib/tools.jar");
      if (toolsJar.isFile()) {
        rootFiles.add(toolsJar);
      }
    }
    for (File jarDir : jarDirs) {
      if (jarDir != null && jarDir.isDirectory()) {
        File[] jarFiles = notNull(jarDir.listFiles(jarFileFilter), ArrayUtil.EMPTY_FILE_ARRAY);
        for (File jarFile : jarFiles) {
          String jarFileName = jarFile.getName();
          if (jarFileName.equals("alt-rt.jar") || jarFileName.equals("alt-string.jar")) {
            continue;  // filter out alternative implementations
          }
          String canonicalPath = getCanonicalPath(jarFile);
          if (canonicalPath == null || !pathFilter.add(canonicalPath)) {
            continue;  // filter out duplicate (symbolically linked) .jar files commonly found in OS X JDK distributions
          }
          rootFiles.add(jarFile);
        }
      }
    }

    String[] ibmJdkVmJarDirs = {"bin/default", "lib/i386/default", "lib/amd64/default", "lib/s390x/default"};
    for (String relativePath : ibmJdkVmJarDirs) {
      File libDir = new File(home, isJre ? relativePath : "jre/" + relativePath);
      File[] vmJarDirs = notNull(libDir.listFiles(FileUtilRt.ALL_DIRECTORIES), ArrayUtil.EMPTY_FILE_ARRAY);
      for (File dir : vmJarDirs) {
        if (dir.getName().startsWith("jclSC")) {
          File vmJar = new File(dir, "vm.jar");
          if (vmJar.isFile()) {
            rootFiles.add(vmJar);
          }
        }
      }
    }
    File classesZip = new File(home, "lib/classes.zip");
    if (classesZip.isFile()) {
      rootFiles.add(classesZip);
    }

    if (rootFiles.isEmpty()) {
      File classesDir = new File(home, "classes");
      if (classesDir.isDirectory()) {
        rootFiles.add(classesDir);
      }
    }

    return rootFiles;
  }

  @Nullable
  private static String getCanonicalPath(File file) {
    try {
      return file.getCanonicalPath();
    }
    catch (IOException e) {
      return null;
    }
  }
}
