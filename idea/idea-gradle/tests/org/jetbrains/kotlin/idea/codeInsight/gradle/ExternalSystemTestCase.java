/*
 * Copyright 2010-2016 JetBrains s.r.o.
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
package org.jetbrains.kotlin.idea.codeInsight.gradle;

import com.intellij.compiler.CompilerTestUtil;
import com.intellij.openapi.application.AccessToken;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.Result;
import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.CharsetToolkit;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.newvfs.impl.VfsRootAccess;
import com.intellij.testFramework.EdtTestUtil;
import com.intellij.testFramework.UsefulTestCase;
import com.intellij.testFramework.fixtures.IdeaProjectTestFixture;
import com.intellij.testFramework.fixtures.IdeaTestFixtureFactory;
import com.intellij.util.ArrayUtil;
import com.intellij.util.Processor;
import com.intellij.util.ThrowableRunnable;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.junit.After;
import org.junit.Before;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

// part of com.intellij.openapi.externalSystem.test.ExternalSystemTestCase
public abstract class ExternalSystemTestCase extends UsefulTestCase {
  private File ourTempDir;

  protected IdeaProjectTestFixture myTestFixture;

  protected Project myProject;

  private File myTestDir;
  private VirtualFile myProjectRoot;
  private final List<VirtualFile> myAllConfigs = new ArrayList<VirtualFile>();

  @Before
  @Override
  public void setUp() throws Exception {
    super.setUp();
    ensureTempDirCreated();

    myTestDir = new File(ourTempDir, getTestName(false));
    FileUtil.ensureExists(myTestDir);

    setUpFixtures();
    myProject = myTestFixture.getProject();

    UIUtil.invokeAndWaitIfNeeded((Runnable) () -> ApplicationManager.getApplication().runWriteAction(() -> {
      try {
        setUpInWriteAction();
      }
      catch (Throwable e) {
        try {
          tearDown();
        }
        catch (Exception e1) {
          e1.printStackTrace();
        }
        throw new RuntimeException(e);
      }
    }));

    List<String> allowedRoots = new ArrayList<String>();
    collectAllowedRoots(allowedRoots);
    if (!allowedRoots.isEmpty()) {
      VfsRootAccess.allowRootAccess(getTestRootDisposable(), ArrayUtil.toStringArray(allowedRoots));
    }

    CompilerTestUtil.enableExternalCompiler();
  }

  protected void collectAllowedRoots(List<String> roots) throws IOException {
  }

  public static Collection<String> collectRootsInside(String root) {
    final List<String> roots = ContainerUtil.newSmartList();
    roots.add(root);
    FileUtil.processFilesRecursively(new File(root), new Processor<File>() {
      @Override
      public boolean process(File file) {
        try {
          String path = file.getCanonicalPath();
          if (!FileUtil.isAncestor(path, path, false)) {
            roots.add(path);
          }
        }
        catch (IOException ignore) {
        }
        return true;
      }
    });

    return roots;
  }

  private void ensureTempDirCreated() throws IOException {
    if (ourTempDir != null) return;

    ourTempDir = new File(FileUtil.getTempDirectory(), getTestsTempDir());
    FileUtil.delete(ourTempDir);
    FileUtil.ensureExists(ourTempDir);
  }

  protected abstract String getTestsTempDir();

  protected void setUpFixtures() throws Exception {
    myTestFixture = IdeaTestFixtureFactory.getFixtureFactory().createFixtureBuilder(getName()).getFixture();
    myTestFixture.setUp();
  }

  private void setUpInWriteAction() throws Exception {
    File projectDir = new File(myTestDir, "project");
    FileUtil.ensureExists(projectDir);
    myProjectRoot = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(projectDir);
  }

  @After
  @Override
  public void tearDown() throws Exception {
    try {
      EdtTestUtil.runInEdtAndWait(new ThrowableRunnable<Throwable>() {
        @Override
        public void run() throws Throwable {
          CompilerTestUtil.disableExternalCompiler(myProject);
          tearDownFixtures();
        }
      });
      myProject = null;
      if (!FileUtil.delete(myTestDir) && myTestDir.exists()) {
        System.err.println("Cannot delete " + myTestDir);
        //printDirectoryContent(myDir);
        myTestDir.deleteOnExit();
      }
    }
    finally {
      super.tearDown();
      resetClassFields(getClass());
    }
  }

  protected void tearDownFixtures() throws Exception {
    myTestFixture.tearDown();
    myTestFixture = null;
  }

  private void resetClassFields(Class<?> aClass) {
    if (aClass == null) return;

    Field[] fields = aClass.getDeclaredFields();
    for (Field field : fields) {
      final int modifiers = field.getModifiers();
      if ((modifiers & Modifier.FINAL) == 0
          && (modifiers & Modifier.STATIC) == 0
          && !field.getType().isPrimitive()) {
        field.setAccessible(true);
        try {
          field.set(this, null);
        }
        catch (IllegalAccessException e) {
          e.printStackTrace();
        }
      }
    }

    if (aClass == ExternalSystemTestCase.class) return;
    resetClassFields(aClass.getSuperclass());
  }

  @Override
  protected void runTest() throws Throwable {
    try {
      super.runTest();
    }
    catch (Exception throwable) {
      Throwable each = throwable;
      do {
        if (each instanceof HeadlessException) {
          printIgnoredMessage("Doesn't work in Headless environment");
          return;
        }
      }
      while ((each = each.getCause()) != null);
      throw throwable;
    }
  }

  @Override
  protected void invokeTestRunnable(@NotNull Runnable runnable) throws Exception {
    runnable.run();
  }

  protected static String getRoot() {
    if (SystemInfo.isWindows) return "c:";
    return "";
  }

  protected String getProjectPath() {
    return myProjectRoot.getPath();
  }

  protected VirtualFile createProjectConfig(@NonNls String config) throws IOException {
    return createConfigFile(myProjectRoot, config);
  }

  private VirtualFile createConfigFile(final VirtualFile dir, String config) throws IOException {
    final String configFileName = getExternalSystemConfigFileName();
    VirtualFile f = dir.findChild(configFileName);
    if (f == null) {
      f = new WriteAction<VirtualFile>() {
        @Override
        protected void run(@NotNull Result<VirtualFile> result) throws Throwable {
          VirtualFile res = dir.createChildData(null, configFileName);
          result.setResult(res);
        }
      }.execute().getResultObject();
      myAllConfigs.add(f);
    }
    setFileContent(f, config, true);
    return f;
  }

  protected abstract String getExternalSystemConfigFileName();

  protected VirtualFile createProjectSubFile(String relativePath) throws IOException {
    File f = new File(getProjectPath(), relativePath);
    FileUtil.ensureExists(f.getParentFile());
    FileUtil.ensureCanCreateFile(f);
    boolean created = f.createNewFile();
    if(!created) {
      throw new AssertionError("Unable to create the project sub file: " + f.getAbsolutePath());
    }
    return LocalFileSystem.getInstance().refreshAndFindFileByIoFile(f);
  }

  protected VirtualFile createProjectSubFile(String relativePath, String content) throws IOException {
    VirtualFile file = createProjectSubFile(relativePath);
    setFileContent(file, content, false);
    return file;
  }

  protected Module getModule(String name) {
    AccessToken accessToken = ApplicationManager.getApplication().acquireReadActionLock();
    try {
      Module m = ModuleManager.getInstance(myProject).findModuleByName(name);
      assertNotNull("Module " + name + " not found", m);
      return m;
    }
    finally {
      accessToken.finish();
    }
  }

  private static void setFileContent(final VirtualFile file, final String content, final boolean advanceStamps) throws IOException {
    new WriteAction<VirtualFile>() {
      @Override
      protected void run(@NotNull Result<VirtualFile> result) throws Throwable {
        if (advanceStamps) {
          file.setBinaryContent(content.getBytes(CharsetToolkit.UTF8_CHARSET), -1, file.getTimeStamp() + 4000);
        }
        else {
          file.setBinaryContent(content.getBytes(CharsetToolkit.UTF8_CHARSET), file.getModificationStamp(), file.getTimeStamp());
        }
      }
    }.execute().getResultObject();
  }

  private void printIgnoredMessage(String message) {
    String toPrint = "Ignored";
    if (message != null) {
      toPrint += ", because " + message;
    }
    toPrint += ": " + getClass().getSimpleName() + "." + getName();
    System.out.println(toPrint);
  }
}
