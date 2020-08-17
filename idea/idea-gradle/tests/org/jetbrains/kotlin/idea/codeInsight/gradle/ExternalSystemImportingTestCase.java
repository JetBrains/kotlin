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

import com.intellij.find.FindManager;
import com.intellij.find.findUsages.FindUsagesHandler;
import com.intellij.find.findUsages.FindUsagesManager;
import com.intellij.find.findUsages.FindUsagesOptions;
import com.intellij.find.impl.FindManagerImpl;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.externalSystem.importing.ImportSpec;
import com.intellij.openapi.externalSystem.importing.ImportSpecBuilder;
import com.intellij.openapi.externalSystem.model.DataNode;
import com.intellij.openapi.externalSystem.model.ProjectSystemId;
import com.intellij.openapi.externalSystem.model.project.ProjectData;
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskId;
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskNotificationListenerAdapter;
import com.intellij.openapi.externalSystem.service.execution.ProgressExecutionMode;
import com.intellij.openapi.externalSystem.service.notification.ExternalSystemProgressNotificationManager;
import com.intellij.openapi.externalSystem.service.project.ExternalProjectRefreshCallback;
import com.intellij.openapi.externalSystem.service.project.ProjectDataManager;
import com.intellij.openapi.externalSystem.settings.AbstractExternalSystemSettings;
import com.intellij.openapi.externalSystem.settings.ExternalProjectSettings;
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil;
import com.intellij.openapi.externalSystem.util.ExternalSystemUtil;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.projectRoots.impl.JavaAwareProjectJdkTableImpl;
import com.intellij.openapi.roots.*;
import com.intellij.openapi.roots.libraries.Library;
import com.intellij.openapi.roots.libraries.LibraryTablesRegistrar;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.ui.TestDialog;
import com.intellij.openapi.util.Couple;
import com.intellij.openapi.util.Ref;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.packaging.artifacts.Artifact;
import com.intellij.packaging.artifacts.ArtifactManager;
import com.intellij.psi.PsiElement;
import com.intellij.testFramework.IdeaTestUtil;
import com.intellij.usageView.UsageInfo;
import com.intellij.util.CommonProcessors;
import com.intellij.util.Function;
import com.intellij.util.PathUtil;
import com.intellij.util.containers.ConcurrentWeakKeySoftValueHashMap;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiPredicate;
import java.util.function.Predicate;

import org.apache.log4j.Level;
import org.jetbrains.jps.model.java.JavaResourceRootType;
import org.jetbrains.jps.model.java.JavaSourceRootProperties;
import org.jetbrains.jps.model.java.JavaSourceRootType;
import org.jetbrains.jps.model.module.JpsModuleSourceRootType;

import static com.intellij.testFramework.EdtTestUtil.runInEdtAndGet;

// part of com.intellij.openapi.externalSystem.test.ExternalSystemImportingTestCase
public abstract class ExternalSystemImportingTestCase extends ExternalSystemTestCase {

    public static final String MINIMAL_SUPPORTED_GRADLE_PLUGIN_VERSION = "1.3.0";
    public static final String LATEST_STABLE_GRADLE_PLUGIN_VERSION = "1.3.72";

    private Logger logger;

    protected abstract ExternalProjectSettings getCurrentExternalProjectSettings();

    protected abstract ProjectSystemId getExternalSystemId();

    // assertion methods
    protected void assertModulesContains(@NotNull Project project, String... expectedNames) {
        Module[] actual = ModuleManager.getInstance(project).getModules();
        List<String> actualNames = new ArrayList<>();

        for (Module m : actual) {
            actualNames.add(m.getName());
        }

        assertContain(actualNames, expectedNames);
    }

    protected void assertModulesContains(String... expectedNames) {
        assertModulesContains(myProject, expectedNames);
    }

    protected void assertModules(@NotNull Project project, String... expectedNames) {
        Module[] actual = ModuleManager.getInstance(project).getModules();
        List<String> actualNames = new ArrayList<>();

        for (Module m : actual) {
            actualNames.add(m.getName());
        }

        assertUnorderedElementsAreEqual(actualNames, expectedNames);
    }

    protected void assertModules(String... expectedNames) {
        assertModules(myProject, expectedNames);
    }

    protected void assertContentRoots(String moduleName, String... expectedRoots) {
        List<String> actual = new ArrayList<>();
        for (ContentEntry e : getContentRoots(moduleName)) {
            actual.add(e.getUrl());
        }

        for (int i = 0; i < expectedRoots.length; i++) {
            expectedRoots[i] = VfsUtilCore.pathToUrl(expectedRoots[i]);
        }

        assertUnorderedPathsAreEqual(actual, Arrays.asList(expectedRoots));
    }

    protected void assertSources(String moduleName, String... expectedSources) {
        doAssertContentFolders(moduleName, JavaSourceRootType.SOURCE, expectedSources);
    }

    protected void assertGeneratedSources(String moduleName, String... expectedSources) {
        assertGeneratedSources(moduleName, JavaSourceRootType.SOURCE, expectedSources);
    }

    protected void assertGeneratedTestSources(String moduleName, String... expectedSources) {
        assertGeneratedSources(moduleName, JavaSourceRootType.TEST_SOURCE, expectedSources);
    }

    private void assertGeneratedSources(String moduleName, JavaSourceRootType type, String... expectedSources) {
        final ContentEntry[] contentRoots = getContentRoots(moduleName);
        String rootUrl = contentRoots.length > 1 ? ExternalSystemApiUtil.getExternalProjectPath(getModule(moduleName)) : null;
        List<String> actual = new ArrayList<>();

        for (ContentEntry contentRoot : contentRoots) {
            rootUrl = VirtualFileManager.extractPath(rootUrl == null ? contentRoot.getUrl() : rootUrl);
            for (SourceFolder f : contentRoot.getSourceFolders(type)) {
                String folderUrl = VirtualFileManager.extractPath(f.getUrl());

                if (folderUrl.startsWith(rootUrl)) {
                    int length = rootUrl.length() + 1;
                    folderUrl = folderUrl.substring(Math.min(length, folderUrl.length()));
                }

                JavaSourceRootProperties properties = f.getJpsElement().getProperties(type);
                if (properties != null && properties.isForGeneratedSources()) {
                    actual.add(folderUrl);
                }
            }
        }

        assertOrderedElementsAreEqual(actual, Arrays.asList(expectedSources));
    }

    protected void assertResources(String moduleName, String... expectedSources) {
        doAssertContentFolders(moduleName, JavaResourceRootType.RESOURCE, expectedSources);
    }

    protected void assertTestSources(String moduleName, String... expectedSources) {
        doAssertContentFolders(moduleName, JavaSourceRootType.TEST_SOURCE, expectedSources);
    }

    protected void assertTestResources(String moduleName, String... expectedSources) {
        doAssertContentFolders(moduleName, JavaResourceRootType.TEST_RESOURCE, expectedSources);
    }

    protected void assertExcludes(String moduleName, String... expectedExcludes) {
        ContentEntry contentRoot = getContentRoot(moduleName);
        doAssertContentFolders(contentRoot, Arrays.asList(contentRoot.getExcludeFolders()), expectedExcludes);
    }

    protected void assertContentRootExcludes(String moduleName, String contentRoot, String... expectedExcudes) {
        ContentEntry root = getContentRoot(moduleName, contentRoot);
        doAssertContentFolders(root, Arrays.asList(root.getExcludeFolders()), expectedExcudes);
    }

    private void doAssertContentFolders(String moduleName, @NotNull JpsModuleSourceRootType<?> rootType, String... expected) {
        final ContentEntry[] contentRoots = getContentRoots(moduleName);
        final String rootUrl = contentRoots.length > 1 ? ExternalSystemApiUtil.getExternalProjectPath(getModule(moduleName)) : null;
        doAssertContentFolders(rootUrl, contentRoots, rootType, expected);
    }

    protected static List<SourceFolder> doAssertContentFolders(@Nullable String rootUrl,
            ContentEntry[] contentRoots,
            @NotNull JpsModuleSourceRootType<?> rootType,
            String... expected) {
        List<SourceFolder> result = new ArrayList<>();
        List<String> actual = new ArrayList<>();
        for (ContentEntry contentRoot : contentRoots) {
            for (SourceFolder f : contentRoot.getSourceFolders(rootType)) {
                rootUrl = VirtualFileManager.extractPath(rootUrl == null ? contentRoot.getUrl() : rootUrl);
                String folderUrl = VirtualFileManager.extractPath(f.getUrl());
                if (folderUrl.startsWith(rootUrl)) {
                    int length = rootUrl.length() + 1;
                    folderUrl = folderUrl.substring(Math.min(length, folderUrl.length()));
                }

                actual.add(folderUrl);
                result.add(f);
            }
        }

        assertOrderedElementsAreEqual(actual, Arrays.asList(expected));
        return result;
    }

    private static void doAssertContentFolders(ContentEntry e, final List<? extends ContentFolder> folders, String... expected) {
        List<String> actual = new ArrayList<>();
        for (ContentFolder f : folders) {
            String rootUrl = e.getUrl();
            String folderUrl = f.getUrl();

            if (folderUrl.startsWith(rootUrl)) {
                int length = rootUrl.length() + 1;
                folderUrl = folderUrl.substring(Math.min(length, folderUrl.length()));
            }

            actual.add(folderUrl);
        }

        assertOrderedElementsAreEqual(actual, Arrays.asList(expected));
    }

    /* Unused but available in ESITC@IDEA
    protected void assertModuleOutputs(String moduleName, String... outputs) {
        String[] outputPaths = ContainerUtil.map2Array(CompilerPaths.getOutputPaths(new Module[]{getModule(moduleName)}), String.class,
                                                       s -> getAbsolutePath(s));
        assertUnorderedElementsAreEqual(outputPaths, outputs);
    }
    */

    protected void assertModuleOutput(String moduleName, String output, String testOutput) {
        CompilerModuleExtension e = getCompilerExtension(moduleName);

        assertFalse(e.isCompilerOutputPathInherited());
        assertEquals(output, getAbsolutePath(e.getCompilerOutputUrl()));
        assertEquals(testOutput, getAbsolutePath(e.getCompilerOutputUrlForTests()));
    }

    protected void assertModuleInheritedOutput(String moduleName) {
        CompilerModuleExtension e = getCompilerExtension(moduleName);
        assertTrue(e.isCompilerOutputPathInherited());
    }

    protected static String getAbsolutePath(String path) {
        path = VfsUtilCore.urlToPath(path);
        path = PathUtil.getCanonicalPath(path);
        return FileUtil.toSystemIndependentName(path);
    }

    protected void assertProjectOutput(String module) {
        assertTrue(getCompilerExtension(module).isCompilerOutputPathInherited());
    }

    protected CompilerModuleExtension getCompilerExtension(String module) {
        return CompilerModuleExtension.getInstance(getModule(module));
    }

    protected void assertModuleLibDep(String moduleName, String depName) {
        assertModuleLibDep(moduleName, depName, null);
    }

    protected void assertModuleLibDep(String moduleName, String depName, String classesPath) {
        assertModuleLibDep(moduleName, depName, classesPath, null, null);
    }

    protected void assertModuleLibDep(String moduleName, String depName, String classesPath, String sourcePath, String javadocPath) {
        LibraryOrderEntry lib = ContainerUtil.getFirstItem(getModuleLibDeps(moduleName, depName));
        final String errorMessage = "Failed to find dependency with name [" + depName + "] in module [" + moduleName + "]\n" +
                                    "Available dependencies: " + collectModuleDepsNames(moduleName, LibraryOrderEntry.class);
        assertNotNull(errorMessage, lib);
        assertModuleLibDepPath(lib, OrderRootType.CLASSES, classesPath == null ? null : Collections.singletonList(classesPath));
        assertModuleLibDepPath(lib, OrderRootType.SOURCES, sourcePath == null ? null : Collections.singletonList(sourcePath));
        assertModuleLibDepPath(lib, JavadocOrderRootType.getInstance(), javadocPath == null ? null : Collections.singletonList(javadocPath));
    }

    protected void assertModuleLibDep(String moduleName,
            String depName,
            List<String> classesPaths,
            List<String> sourcePaths,
            List<String> javadocPaths) {
        LibraryOrderEntry lib = ContainerUtil.getFirstItem(getModuleLibDeps(moduleName, depName));

        assertModuleLibDepPath(lib, OrderRootType.CLASSES, classesPaths);
        assertModuleLibDepPath(lib, OrderRootType.SOURCES, sourcePaths);
        assertModuleLibDepPath(lib, JavadocOrderRootType.getInstance(), javadocPaths);
    }

    private static void assertModuleLibDepPath(LibraryOrderEntry lib, OrderRootType type, List<String> paths) {
        assertNotNull(lib);
        if (paths == null) return;
        assertUnorderedPathsAreEqual(Arrays.asList(lib.getRootUrls(type)), paths);
        // also check the library because it may contain slight different set of urls (e.g. with duplicates)
        final Library library = lib.getLibrary();
        assertNotNull(library);
        assertUnorderedPathsAreEqual(Arrays.asList(library.getUrls(type)), paths);
    }

    protected void assertModuleLibDepScope(String moduleName, String depName, DependencyScope... scopes) {
        List<LibraryOrderEntry> deps = getModuleLibDeps(moduleName, depName);
        assertUnorderedElementsAreEqual(ContainerUtil.map2Array(deps, entry -> entry.getScope()), scopes);
    }

    protected List<LibraryOrderEntry> getModuleLibDeps(String moduleName, String depName) {
        return getModuleDep(moduleName, depName, LibraryOrderEntry.class);
    }

    protected void assertModuleLibDeps(String moduleName, String... expectedDeps) {
        assertModuleDeps(moduleName, LibraryOrderEntry.class, expectedDeps);
    }

    protected void assertModuleLibDeps(BiPredicate<String, String> predicate, String moduleName, String... expectedDeps) {
        assertModuleDeps(predicate, moduleName, LibraryOrderEntry.class, expectedDeps);
    }

    protected void assertExportedDeps(String moduleName, String... expectedDeps) {
        final List<String> actual = new ArrayList<>();

        getRootManager(moduleName).orderEntries().withoutSdk().withoutModuleSourceEntries().exportedOnly().process(new RootPolicy<Object>() {
            @Override
            public Object visitModuleOrderEntry(@NotNull ModuleOrderEntry e, Object value) {
                actual.add(e.getModuleName());
                return null;
            }

            @Override
            public Object visitLibraryOrderEntry(@NotNull LibraryOrderEntry e, Object value) {
                actual.add(e.getLibraryName());
                return null;
            }
        }, null);

        assertOrderedElementsAreEqual(actual, expectedDeps);
    }

    protected void assertModuleModuleDeps(String moduleName, String... expectedDeps) {
        assertModuleDeps(moduleName, ModuleOrderEntry.class, expectedDeps);
    }

    private void assertModuleDeps(String moduleName, Class clazz, String... expectedDeps) {
        assertModuleDeps(equalsPredicate(), moduleName, clazz, expectedDeps);
    }

    private void assertModuleDeps(BiPredicate<String, String> predicate, String moduleName, Class clazz, String... expectedDeps) {
        assertOrderedElementsAreEqual(predicate, collectModuleDepsNames(moduleName, clazz), expectedDeps);
    }

    /* Unused but available in ESITC@IDEA
    protected void assertProductionOnTestDependencies(String moduleName, String... expectedDeps) {
        assertOrderedElementsAreEqual(collectModuleDepsNames(
                moduleName, entry -> entry instanceof ModuleOrderEntry && ((ModuleOrderEntry)entry).isProductionOnTestDependency()
        ), expectedDeps);
    }
    */

    protected void assertModuleModuleDepScope(String moduleName, String depName, DependencyScope... scopes) {
        List<ModuleOrderEntry> deps = getModuleModuleDeps(moduleName, depName);
        assertUnorderedElementsAreEqual(ContainerUtil.map2Array(deps, entry -> entry.getScope()), scopes);
    }

    protected void assertNoModuleDepForModule(String moduleName, String depName) {
        assertEmpty("No dependency '" + depName + "' was expected", collectModuleDeps(moduleName, depName, ModuleOrderEntry.class));
    }

    protected void assertNoLibraryDepForModule(String moduleName, String depName) {
        assertEmpty("No dependency '" + depName + "' was expected", collectModuleDeps(moduleName, depName, LibraryOrderEntry.class));
    }

    public void assertProjectLibraries(String... expectedNames) {
        List<String> actualNames = new ArrayList<>();
        for (Library each : LibraryTablesRegistrar.getInstance().getLibraryTable(myProject).getLibraries()) {
            String name = each.getName();
            actualNames.add(name == null ? "<unnamed>" : name);
        }
        assertUnorderedElementsAreEqual(actualNames, expectedNames);
    }

    protected void assertModuleGroupPath(String moduleName, String... expected) {
        String[] path = ModuleManager.getInstance(myProject).getModuleGroupPath(getModule(moduleName));

        if (expected.length == 0) {
            assertNull(path);
        }
        else {
            assertNotNull(path);
            assertOrderedElementsAreEqual(Arrays.asList(path), expected);
        }
    }

    protected void assertArtifacts(String... expectedNames) {
        final List<String> actualNames = ContainerUtil.map(
                ArtifactManager.getInstance(myProject).getAllArtifactsIncludingInvalid(),
                (Function<Artifact, String>)artifact -> artifact.getName());

        assertUnorderedElementsAreEqual(actualNames, expectedNames);
    }

    //end of assertions

    private ContentEntry getContentRoot(String moduleName) {
        ContentEntry[] ee = getContentRoots(moduleName);
        List<String> roots = new ArrayList<>();
        for (ContentEntry e : ee) {
            roots.add(e.getUrl());
        }

        String message = "Several content roots found: [" + StringUtil.join(roots, ", ") + "]";
        assertEquals(message, 1, ee.length);

        return ee[0];
    }

    private ContentEntry getContentRoot(String moduleName, String path) {
        for (ContentEntry e : getContentRoots(moduleName)) {
            if (e.getUrl().equals(VfsUtilCore.pathToUrl(path))) return e;
        }
        throw new AssertionError("content root not found");
    }

    public ContentEntry[] getContentRoots(String moduleName) {
        return getRootManager(moduleName).getContentEntries();
    }

    protected void importProject(@NonNls String config) throws IOException {
        createProjectConfig(config);
        importProject();
    }

    protected void importProject() {
        doImportProject();
    }

    public boolean ensureIsNotGradleProxyObject(
            Object o,
            Map<Object, Object> referencingObjects,
            Map<Object, String> referencingFieldNames
    ) {
        if (!(o instanceof Proxy)) {
            return true;
        }
        StringBuilder errMessage = new StringBuilder();
        errMessage.append(String.format(
                "Object [%s] seems to be a referenced gradle tooling api object. (it may lead to memory leaks during import) Referencing path: ",
                o));
        while (o != null) {
            errMessage.append(String.format("[%s].[%s] type: %s <-\r\n", o, referencingFieldNames.get(o), o.getClass().toString()));
            o = referencingObjects.get(o);
        }
        System.err.println(errMessage.toString());
        //TODO uncomment after fixing IDEA-207782
        //fail(errMessage.toString());
        return false;
    }

    private boolean shouldBeProcessed(Object toProcess, Set<Object> processed) {
        return toProcess != null && !processed.contains(toProcess);
    }

    public void inspectForGradleMemoryLeaks(DataNode<ProjectData> externalProject) {
        // Static logger initialisation should not be used because logger factory may be changed on test.setUp
        if (logger == null) {
            logger = Logger.getInstance(this.getClass().getName());
            logger.setLevel(Level.INFO);//not required
        }
        long start = System.currentTimeMillis();
        Set<Object> processed = new HashSet<>();
        Queue<Object> toProcess = new LinkedList<>();
        Map<Object, Object> referencingObjects = new HashMap<>();
        Map<Object, String> referencingFieldNames = new HashMap<>();
        Set<Field> modifiedFields = new HashSet<>();
        toProcess.add(externalProject);
        try {
            while (!toProcess.isEmpty()) {
                Object nextObject = toProcess.poll();
                processed.add(nextObject);
                if (!ensureIsNotGradleProxyObject(nextObject, referencingObjects, referencingFieldNames)) {
                    break;
                }

                for (Field field : nextObject.getClass().getDeclaredFields()) {
                    try {

                        if (!field.isAccessible()) {
                            field.setAccessible(true);
                            modifiedFields.add(field);
                        }
                        final Object fieldValue = field.get(nextObject);
                        if (fieldValue == null || fieldValue.getClass().isPrimitive()) {
                            continue;
                        }
                        if (fieldValue instanceof Collection) {
                            for (Object o : (Collection) fieldValue) {
                                saveToProcessIfRequired(processed, toProcess, referencingObjects, referencingFieldNames, nextObject, o,
                                                        field.getName());
                            }
                        }
                        else if (fieldValue.getClass().isArray()) {
                            for (int i = 0; i < Array.getLength(fieldValue); i++) {
                                Object o = Array.get(fieldValue, i);
                                saveToProcessIfRequired(processed, toProcess, referencingObjects, referencingFieldNames, nextObject, o,
                                                        field.getName());
                            }
                        }
                        else if (fieldValue instanceof Map && ! (fieldValue instanceof ConcurrentWeakKeySoftValueHashMap)) {
                            for (Map.Entry e : ((Map<Object, Object>) fieldValue).entrySet()) {
                                saveToProcessIfRequired(processed, toProcess, referencingObjects, referencingFieldNames, nextObject, e.getKey(),
                                                        field.getName());
                                saveToProcessIfRequired(processed, toProcess, referencingObjects, referencingFieldNames, nextObject, e.getValue(),
                                                        "value" + field.getName());
                            }
                        }
                        else {
                            saveToProcessIfRequired(processed, toProcess, referencingObjects, referencingFieldNames, nextObject, fieldValue,
                                                    field.getName());
                        }
                    }
                    catch (IllegalAccessException e) {
                        fail(e.getMessage());
                    }
                }
            }
        }
        finally {
            for (Field f : modifiedFields) {
                f.setAccessible(false);
            }
            logger.info(
                    String.format("Memory leak tracker has finished. Number of processed objects = %s. Duration = %s ms.", processed.size(),
                                  (System.currentTimeMillis() - start)));
        }
    }

    private void saveToProcessIfRequired(
            Set<Object> processed,
            Queue<Object> toProcess,
            Map<Object, Object> referrers,
            Map<Object, String> referencingFieldNames,
            Object referringObject,
            Object o,
            String fieldName
    ) {
        if (shouldBeProcessed(o, processed)) {
            toProcess.add(o);
            referencingFieldNames.put(o, fieldName);
            referrers.put(o, referringObject);
        }
    }

    private void doImportProject() {
        AbstractExternalSystemSettings systemSettings = ExternalSystemApiUtil.getSettings(myProject, getExternalSystemId());
        final ExternalProjectSettings projectSettings = getCurrentExternalProjectSettings();
        projectSettings.setExternalProjectPath(getProjectPath());
        //noinspection unchecked
        Set<ExternalProjectSettings> projects = new HashSet<>(systemSettings.getLinkedProjectsSettings());
        projects.remove(projectSettings);
        projects.add(projectSettings);
        //noinspection unchecked
        systemSettings.setLinkedProjectsSettings(projects);

        final Ref<Couple<String>> error = Ref.create();
        ImportSpec importSpec = createImportSpec();
        ExternalProjectRefreshCallback callback = importSpec.getCallback();
        if (callback == null || callback instanceof ImportSpecBuilder.DefaultProjectRefreshCallback) {
            importSpec = new TestImportSpecBuilder(importSpec)
                    .setCreateEmptyContentRoots(projectSettings.isCreateEmptyContentRootDirectories())
                    .callback(new ExternalProjectRefreshCallback() {
                @Override
                public void onSuccess(@Nullable final DataNode<ProjectData> externalProject) {
                    if (externalProject == null) {
                        System.err.println("Got null External project after import");
                        return;
                    }
                    ServiceManager.getService(ProjectDataManager.class).importData(externalProject, myProject, true);
                    System.out.println("External project was successfully imported");

                    inspectForGradleMemoryLeaks(externalProject);
                }

                @Override
                public void onFailure(@NotNull String errorMessage, @Nullable String errorDetails) {
                    error.set(Couple.of(errorMessage, errorDetails));
                }
            }).build();
        }

        ExternalSystemProgressNotificationManager notificationManager =
                ServiceManager.getService(ExternalSystemProgressNotificationManager.class);
        ExternalSystemTaskNotificationListenerAdapter listener = new ExternalSystemTaskNotificationListenerAdapter() {
            @Override
            public void onTaskOutput(@NotNull ExternalSystemTaskId id, @NotNull String text, boolean stdOut) {
                if (StringUtil.isEmptyOrSpaces(text)) return;
                (stdOut ? System.out : System.err).print(text);
            }
        };
        notificationManager.addNotificationListener(listener);
        try {
            ExternalSystemUtil.refreshProjects(importSpec);
        }
        finally {
            notificationManager.removeNotificationListener(listener);
        }

        if (!error.isNull()) {
            handleImportFailure(error.get().first, error.get().second);
        }
    }

    protected void handleImportFailure(@NotNull String errorMessage, @Nullable String errorDetails) {
        String failureMsg = "Import failed: " + errorMessage;
        if (StringUtil.isNotEmpty(errorDetails)) {
            failureMsg += "\nError details: \n" + errorDetails;
        }
        fail(failureMsg);
    }

    protected ImportSpec createImportSpec() {
        ImportSpecBuilder importSpecBuilder = new ImportSpecBuilder(myProject, getExternalSystemId())
                .use(ProgressExecutionMode.MODAL_SYNC)
                .forceWhenUptodate();
        return importSpecBuilder.build();
    }

    protected void setupJdkForModules(String... moduleNames) {
        for (String each : moduleNames) {
            setupJdkForModule(each);
        }
    }

    @Override
    protected Sdk setupJdkForModule(final String moduleName) {
        final Sdk sdk = true ? JavaAwareProjectJdkTableImpl.getInstanceEx().getInternalJdk() : createJdk("Java 1.5");
        ModuleRootModificationUtil.setModuleSdk(getModule(moduleName), sdk);
        return sdk;
    }

    protected static Sdk createJdk(String versionName) {
        return IdeaTestUtil.getMockJdk17(versionName);
    }

    protected static AtomicInteger configConfirmationForYesAnswer() {
        final AtomicInteger counter = new AtomicInteger();
        Messages.setTestDialog(new TestDialog() {
            @Override
            public int show(@NotNull String message) {
                counter.set(counter.get() + 1);
                return 0;
            }
        });
        return counter;
    }

    protected static AtomicInteger configConfirmationForNoAnswer() {
        final AtomicInteger counter = new AtomicInteger();
        Messages.setTestDialog(new TestDialog() {
            @Override
            public int show(@NotNull String message) {
                counter.set(counter.get() + 1);
                return 1;
            }
        });
        return counter;
    }

    protected static Collection<UsageInfo> findUsages(@NotNull PsiElement element) throws Exception {
        return ProgressManager.getInstance().run(new Task.WithResult<Collection<UsageInfo>, Exception>(null, "", false) {
            @Override
            protected Collection<UsageInfo> compute(@NotNull ProgressIndicator indicator) {
                return runInEdtAndGet(() -> {
                    FindUsagesManager findUsagesManager = ((FindManagerImpl) FindManager.getInstance(element.getProject())).getFindUsagesManager();
                    FindUsagesHandler handler = findUsagesManager.getFindUsagesHandler(element, false);
                    assertNotNull(handler);
                    final FindUsagesOptions options = handler.getFindUsagesOptions();
                    final CommonProcessors.CollectProcessor<UsageInfo> processor = new CommonProcessors.CollectProcessor<>();
                    for (PsiElement element : handler.getPrimaryElements()) {
                        handler.processElementUsages(element, processor, options);
                    }
                    for (PsiElement element : handler.getSecondaryElements()) {
                        handler.processElementUsages(element, processor, options);
                    }
                    return processor.getResults();
                });
            }
        });
    }

    @Nullable
    protected SourceFolder findSource(@NotNull String moduleName, @NotNull String sourcePath) {
        return findSource(getRootManager(moduleName), sourcePath);
    }

    @Nullable
    protected SourceFolder findSource(@NotNull ModuleRootModel moduleRootManager, @NotNull String sourcePath) {
        ContentEntry[] contentRoots = moduleRootManager.getContentEntries();
        Module module = moduleRootManager.getModule();
        String rootUrl = getAbsolutePath(ExternalSystemApiUtil.getExternalProjectPath(module));
        for (ContentEntry contentRoot : contentRoots) {
            for (SourceFolder f : contentRoot.getSourceFolders()) {
                String folderPath = getAbsolutePath(f.getUrl());
                String rootPath = getAbsolutePath(rootUrl + "/" + sourcePath);
                if (folderPath.equals(rootPath)) return f;
            }
        }
        return null;
    }


    @NotNull
    private List<ModuleOrderEntry> getModuleModuleDeps(@NotNull String moduleName, @NotNull String depName) {
        return getModuleDep(moduleName, depName, ModuleOrderEntry.class);
    }

    protected ModuleRootManager getRootManager(String module) {
        return ModuleRootManager.getInstance(getModule(module));
    }

    /* Unused but available in ESITC@IDEA
    protected void ignoreData(BooleanFunction<DataNode<?>> booleanFunction, final boolean ignored) {
        final ExternalProjectInfo externalProjectInfo = ProjectDataManagerImpl.getInstance().getExternalProjectData(
                myProject, getExternalSystemId(), getCurrentExternalProjectSettings().getExternalProjectPath());
        assertNotNull(externalProjectInfo);

        final DataNode<ProjectData> projectDataNode = externalProjectInfo.getExternalProjectStructure();
        assertNotNull(projectDataNode);

        final Collection<DataNode<?>> nodes = ExternalSystemApiUtil.findAllRecursively(projectDataNode, booleanFunction);
        for (DataNode<?> node : nodes) {
            node.visit(dataNode -> dataNode.setIgnored(ignored));
        }
        ServiceManager.getService(ProjectDataManager.class).importData(projectDataNode, myProject, true);
    }
    */

    @NotNull
    private <T> List<T> getModuleDep(@NotNull String moduleName, @NotNull String depName, @NotNull Class<T> clazz) {
        List<T> deps = new ArrayList<>();

        for (OrderEntry e : getRootManager(moduleName).getOrderEntries()) {
            if (clazz.isInstance(e) && e.getPresentableName().equals(depName)) {
                deps.add((T)e);
            }
        }
        assertNotNull("Dependency for module \"" + moduleName + "\" not found: " + depName + "\namong: " + collectModuleDepsNames(moduleName, clazz), deps);
        return deps;
    }

    @NotNull
    private <T> List<T> collectModuleDeps(@NotNull String moduleName, @NotNull String depName, @NotNull Class<T> clazz) {
        List<T> deps = ContainerUtil.newArrayList();

        for (OrderEntry e : getRootManager(moduleName).getOrderEntries()) {
            if (clazz.isInstance(e) && e.getPresentableName().equals(depName)) {
                deps.add((T) e);
            }
        }

        return deps;
    }

    private List<String> collectModuleDepsNames(String moduleName, Predicate<OrderEntry> predicate) {
        List<String> actual = new ArrayList<>();

        for (OrderEntry e : getRootManager(moduleName).getOrderEntries()) {
            if (predicate.test(e)) {
                actual.add(e.getPresentableName());
            }
        }
        return actual;
    }

    private List<String> collectModuleDepsNames(String moduleName, Class clazz) {
        return collectModuleDepsNames(moduleName, entry -> clazz.isInstance(entry));
    }

}
