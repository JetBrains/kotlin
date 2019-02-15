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

import com.intellij.openapi.application.AccessToken;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.externalSystem.importing.ImportSpecBuilder;
import com.intellij.openapi.externalSystem.model.DataNode;
import com.intellij.openapi.externalSystem.model.ProjectSystemId;
import com.intellij.openapi.externalSystem.model.project.ProjectData;
import com.intellij.openapi.externalSystem.service.execution.ProgressExecutionMode;
import com.intellij.openapi.externalSystem.service.project.ExternalProjectRefreshCallback;
import com.intellij.openapi.externalSystem.service.project.ProjectDataManager;
import com.intellij.openapi.externalSystem.settings.AbstractExternalSystemSettings;
import com.intellij.openapi.externalSystem.settings.ExternalProjectSettings;
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil;
import com.intellij.openapi.externalSystem.util.ExternalSystemUtil;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.roots.DependencyScope;
import com.intellij.openapi.roots.ModuleOrderEntry;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.roots.OrderEntry;
import com.intellij.openapi.util.Couple;
import com.intellij.openapi.util.Ref;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.containers.ContainerUtilRt;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.*;

// part of com.intellij.openapi.externalSystem.test.ExternalSystemImportingTestCase
public abstract class ExternalSystemImportingTestCase extends ExternalSystemTestCase {
    @Override
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

    protected void importProject(@NonNls String config) throws IOException {
        createProjectConfig(config);
        importProject();
    }

    protected void importProject() {
        doImportProject();
    }

    public void ensureIsNotGradleProxyObject(Object o, Map<Object, Object> referencingObjects) {
        if (o instanceof Proxy) {
            StringBuilder errMessage = new StringBuilder();
            errMessage.append(String.format("Object [%s] seems to be a referenced gradle tooling api object. (it may lead to memory leaks during import) Referencing path: ", o));
            while (o != null) {
                errMessage.append(String.format("[%s] type: %s <-\r\n", o, o.getClass().toString()));
                o = referencingObjects.get(o);
            }
            //TODO fail(errMessage.toString());
        }
    }

    private boolean shouldBeProcessed(Object toProcess, Set<Object> processed) {
        return toProcess != null && !processed.contains(toProcess);
    }

    public void inspectForGradleMemoryLeaks(DataNode<ProjectData> externalProject) {
        long start = System.currentTimeMillis();
        Set<Object> processed = new HashSet<>();
        Queue<Object> toProcess = new LinkedList<>();
        Map<Object, Object> referencingObjects = new HashMap<>();
        toProcess.add(externalProject);
        try {
            while (! toProcess.isEmpty()) {
                Object nextObject = toProcess.poll();
                processed.add(nextObject);
                ensureIsNotGradleProxyObject(nextObject, referencingObjects);

                for (Field field : nextObject.getClass().getDeclaredFields()) {
                    try {
                        final Object fieldValue;
                        boolean isAccessible = field.isAccessible();
                        try {
                            field.setAccessible(true);
                            fieldValue = field.get(nextObject);
                        } finally {
                            field.setAccessible(isAccessible);
                        }
                        if (fieldValue == null) {
                            continue;
                        }
                        if (fieldValue instanceof Collection) {
                            for (Object o : (Collection)fieldValue) {
                                saveToProcessIfRequired(processed, toProcess, referencingObjects, nextObject, o);
                            }
                        } else if (fieldValue.getClass().isArray()) {
                            for (int i = 0; i < Array.getLength(fieldValue); i++) {
                                Object o = Array.get(fieldValue, i);
                                saveToProcessIfRequired(processed, toProcess, referencingObjects, nextObject, o);
                            }
                        } else {
                            saveToProcessIfRequired(processed, toProcess, referencingObjects, nextObject, fieldValue);
                        }
                    } catch (IllegalAccessException e) {
                        fail(e.getMessage());
                    }
                }

            }
        } finally {
            System.out.println("Processed size = " + processed.size() + " Duration: " + (System.currentTimeMillis() - start));
        }
    }

    private void saveToProcessIfRequired(Set<Object> processed, Queue<Object> toProcess, Map<Object, Object> referrers, Object referringObject, Object o) {
        if (shouldBeProcessed(o, processed)) {
            toProcess.add(o);
            referrers.put(o, referringObject);
        }
    }

    private void doImportProject() {
        AbstractExternalSystemSettings systemSettings = ExternalSystemApiUtil.getSettings(myProject, getExternalSystemId());
        ExternalProjectSettings projectSettings = getCurrentExternalProjectSettings();
        projectSettings.setExternalProjectPath(getProjectPath());
        @SuppressWarnings("unchecked") Set<ExternalProjectSettings> projects =
                ContainerUtilRt.newHashSet(systemSettings.getLinkedProjectsSettings());
        projects.remove(projectSettings);
        projects.add(projectSettings);
        //noinspection unchecked
        systemSettings.setLinkedProjectsSettings(projects);

        final Ref<Couple<String>> error = Ref.create();
        ExternalSystemUtil.refreshProjects(
                new ImportSpecBuilder(myProject, getExternalSystemId())
                        .use(ProgressExecutionMode.MODAL_SYNC)
                        .callback(new ExternalProjectRefreshCallback() {
                            @Override
                            public void onSuccess(@Nullable DataNode<ProjectData> externalProject) {
                                if (externalProject == null) {
                                    System.err.println("Got null External project after import");
                                    return;
                                }
                                inspectForGradleMemoryLeaks(externalProject);
                                ServiceManager.getService(ProjectDataManager.class).importData(externalProject, myProject, true);
                                System.out.println("External project was successfully imported");
                            }

                            @Override
                            public void onFailure(@NotNull String errorMessage, @Nullable String errorDetails) {
                                error.set(Couple.of(errorMessage, errorDetails));
                            }
                        })
                        .forceWhenUptodate()
        );

        if (!error.isNull()) {
            String failureMsg = "Import failed: " + error.get().first;
            if (StringUtil.isNotEmpty(error.get().second)) {
                failureMsg += "\nError details: \n" + error.get().second;
            }
            fail(failureMsg);
        }
    }

    protected abstract ExternalProjectSettings getCurrentExternalProjectSettings();

    protected abstract ProjectSystemId getExternalSystemId();

    protected void assertModuleModuleDepScope(String moduleName, String depName, DependencyScope... scopes) {
        List<ModuleOrderEntry> deps = getModuleModuleDeps(moduleName, depName);
        Set<DependencyScope> actualScopes = new HashSet<DependencyScope>();
        for (ModuleOrderEntry dep : deps) {
            actualScopes.add(dep.getScope());
        }
        HashSet<DependencyScope> expectedScopes = new HashSet<DependencyScope>(Arrays.asList(scopes));
        assertEquals("Dependency '" + depName + "' for module '" + moduleName + "' has unexpected scope",
                     expectedScopes, actualScopes);
    }

    protected void assertNoDepForModule(String moduleName, String depName) {
        assertEmpty("No dependency '" + depName + "' was expected", collectModuleDeps(moduleName, depName, ModuleOrderEntry.class));
    }

    @NotNull
    private List<ModuleOrderEntry> getModuleModuleDeps(@NotNull String moduleName, @NotNull String depName) {
        return getModuleDep(moduleName, depName, ModuleOrderEntry.class);
    }

    protected ModuleRootManager getRootManager(String module) {
        return ModuleRootManager.getInstance(getModule(module));
    }

    @NotNull
    private <T> List<T> getModuleDep(@NotNull String moduleName, @NotNull String depName, @NotNull Class<T> clazz) {
        List<T> deps = collectModuleDeps(moduleName, depName, clazz);
        assertTrue("Dependency '" +
                   depName +
                   "' for module '" +
                   moduleName +
                   "' not found among: " +
                   collectModuleDepsNames(moduleName, clazz),
                   !deps.isEmpty());
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

    private List<String> collectModuleDepsNames(String moduleName, Class clazz) {
        List<String> actual = new ArrayList<String>();

        for (OrderEntry e : getRootManager(moduleName).getOrderEntries()) {
            if (clazz.isInstance(e)) {
                actual.add(e.getPresentableName());
            }
        }
        return actual;
    }
}
