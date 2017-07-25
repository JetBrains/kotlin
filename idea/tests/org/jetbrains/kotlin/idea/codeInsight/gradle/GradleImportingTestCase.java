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

import com.intellij.compiler.server.BuildManager;
import com.intellij.openapi.application.PathManager;
import com.intellij.openapi.application.Result;
import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.externalSystem.model.ProjectSystemId;
import com.intellij.openapi.externalSystem.model.settings.ExternalSystemExecutionSettings;
import com.intellij.openapi.externalSystem.settings.ExternalSystemSettingsListenerAdapter;
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil;
import com.intellij.openapi.projectRoots.JavaSdk;
import com.intellij.openapi.projectRoots.ProjectJdkTable;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.projectRoots.impl.SdkConfigurationUtil;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.ui.TestDialog;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.testFramework.IdeaTestUtil;
import com.intellij.util.PathUtil;
import com.intellij.util.containers.ContainerUtil;
import org.gradle.util.GradleVersion;
import org.gradle.wrapper.GradleWrapperMain;
import org.intellij.lang.annotations.Language;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.gradle.settings.DistributionType;
import org.jetbrains.plugins.gradle.settings.GradleProjectSettings;
import org.jetbrains.plugins.gradle.settings.GradleSettings;
import org.jetbrains.plugins.gradle.util.GradleConstants;
import org.junit.Rule;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Properties;

import static org.junit.Assume.assumeThat;

// part of org.jetbrains.plugins.gradle.importing.GradleImportingTestCase
@RunWith(value = Parameterized.class)
public abstract class GradleImportingTestCase extends ExternalSystemImportingTestCase {
  private static final String GRADLE_JDK_NAME = "Gradle JDK";
  private static final int GRADLE_DAEMON_TTL_MS = 10000;

  @Rule public TestName name = new TestName();

  @Rule public VersionMatcherRule versionMatcherRule = new VersionMatcherRule();

  @SuppressWarnings({"NullableProblems", "WeakerAccess"}) @NotNull
  @Parameterized.Parameter() public String gradleVersion;

  private GradleProjectSettings myProjectSettings;
  private String myJdkHome;

  @Override
  public void setUp() throws Exception {
    myJdkHome = IdeaTestUtil.requireRealJdkHome();
    super.setUp();
    assumeThat(gradleVersion, versionMatcherRule.getMatcher());
    new WriteAction() {
      @Override
      protected void run(@NotNull Result result) throws Throwable {
        Sdk oldJdk = ProjectJdkTable.getInstance().findJdk(GRADLE_JDK_NAME);
        if (oldJdk != null) {
          ProjectJdkTable.getInstance().removeJdk(oldJdk);
        }
        VirtualFile jdkHomeDir = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(new File(myJdkHome));
        assert jdkHomeDir != null;
        Sdk jdk = SdkConfigurationUtil.setupSdk(new Sdk[0], jdkHomeDir, JavaSdk.getInstance(), true, null, GRADLE_JDK_NAME);
        assertNotNull("Cannot create JDK for " + myJdkHome, jdk);
        ProjectJdkTable.getInstance().addJdk(jdk);
      }
    }.execute();
    myProjectSettings = new GradleProjectSettings();
    GradleSettings.getInstance(myProject).setGradleVmOptions("-Xmx128m -XX:MaxPermSize=64m");
    System.setProperty(ExternalSystemExecutionSettings.REMOTE_PROCESS_IDLE_TTL_IN_MS_KEY, String.valueOf(GRADLE_DAEMON_TTL_MS));
    configureWrapper();
  }

  @Override
  public void tearDown() throws Exception {
    if (myJdkHome == null) {
      //super.setUp() wasn't called
      return;
    }

    try {
      new WriteAction() {
        @Override
        protected void run(@NotNull Result result) throws Throwable {
          Sdk old = ProjectJdkTable.getInstance().findJdk(GRADLE_JDK_NAME);
          if (old != null) {
            SdkConfigurationUtil.removeSdk(old);
          }
        }
      }.execute();
      Messages.setTestDialog(TestDialog.DEFAULT);
      FileUtil.delete(BuildManager.getInstance().getBuildSystemDirectory());
    }
    finally {
      super.tearDown();
    }
  }

  @Override
  protected void collectAllowedRoots(List<String> roots) throws IOException {
    roots.add(myJdkHome);
    roots.addAll(collectRootsInside(myJdkHome));
    roots.add(PathManager.getConfigPath());
  }

  @Override
  public String getName() {
    return name.getMethodName() == null ? super.getName() : FileUtil.sanitizeFileName(name.getMethodName());
  }

  @Parameterized.Parameters(name = "{index}: with Gradle-{0}")
  public static Collection<Object[]> data() throws Throwable {
    return Arrays.asList(AbstractModelBuilderTest.SUPPORTED_GRADLE_VERSIONS);
  }

  @Override
  protected String getTestsTempDir() {
    return "gradleImportTests";
  }

  @Override
  protected String getExternalSystemConfigFileName() {
    return "build.gradle";
  }

  protected void importProjectUsingSingeModulePerGradleProject() {
    getCurrentExternalProjectSettings().setResolveModulePerSourceSet(false);
    importProject();
  }

  @Override
  protected void importProject() {
    ExternalSystemApiUtil.subscribe(myProject, GradleConstants.SYSTEM_ID, new ExternalSystemSettingsListenerAdapter() {
      @Override
      public void onProjectsLinked(@NotNull Collection settings) {
        Object item = ContainerUtil.getFirstItem(settings);
        if (item instanceof GradleProjectSettings) {
          ((GradleProjectSettings)item).setGradleJvm(GRADLE_JDK_NAME);
        }
      }
    });
    super.importProject();
  }

  @Override
  protected void importProject(@NonNls @Language("Groovy") String config) throws IOException {
    config = "allprojects {\n" +
              "  repositories {\n" +
              "    maven {\n" +
              "        url 'http://maven.labs.intellij.net/repo1'\n" +
              "    }\n" +
              "  }" +
              "}\n" + config;
    super.importProject(config);
  }

  @Override
  protected GradleProjectSettings getCurrentExternalProjectSettings() {
    return myProjectSettings;
  }

  @Override
  protected ProjectSystemId getExternalSystemId() {
    return GradleConstants.SYSTEM_ID;
  }

  private void configureWrapper() throws IOException, URISyntaxException {
    URI distributionUri = new AbstractModelBuilderTest.DistributionLocator().getDistributionFor(GradleVersion.version(gradleVersion));

    myProjectSettings.setDistributionType(DistributionType.DEFAULT_WRAPPED);
    final VirtualFile wrapperJarFrom = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(wrapperJar());
    assert wrapperJarFrom != null;

    final VirtualFile wrapperJarFromTo = createProjectSubFile("gradle/wrapper/gradle-wrapper.jar");
    new WriteAction() {
      @Override
      protected void run(@NotNull Result result) throws Throwable {
        wrapperJarFromTo.setBinaryContent(wrapperJarFrom.contentsToByteArray());
      }
    }.execute().throwException();


    Properties properties = new Properties();
    properties.setProperty("distributionBase", "GRADLE_USER_HOME");
    properties.setProperty("distributionPath", "wrapper/dists");
    properties.setProperty("zipStoreBase", "GRADLE_USER_HOME");
    properties.setProperty("zipStorePath", "wrapper/dists");
    properties.setProperty("distributionUrl", distributionUri.toString());

    StringWriter writer = new StringWriter();
    properties.store(writer, null);

    createProjectSubFile("gradle/wrapper/gradle-wrapper.properties", writer.toString());
  }

  @NotNull
  private static File wrapperJar() {
    return new File(PathUtil.getJarPathForClass(GradleWrapperMain.class));
  }
}
