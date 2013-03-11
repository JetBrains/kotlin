/*
 * Copyright 2010-2013 JetBrains s.r.o.
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

package org.jetbrains.jet.plugin.project;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.roots.*;
import com.intellij.openapi.roots.libraries.Library;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.util.CachedValueProvider;
import com.intellij.psi.util.CachedValuesManager;
import com.intellij.util.Processor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.PlatformToKotlinClassMap;
import org.jetbrains.jet.lang.descriptors.ModuleDescriptor;
import org.jetbrains.jet.lang.descriptors.SubModuleDescriptor;
import org.jetbrains.jet.lang.descriptors.impl.MutableModuleDescriptor;
import org.jetbrains.jet.lang.descriptors.impl.MutableSubModuleDescriptor;
import org.jetbrains.jet.lang.resolve.KotlinModuleManager;
import org.jetbrains.jet.lang.resolve.java.JavaToKotlinClassMap;
import org.jetbrains.jet.lang.resolve.name.Name;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class IdeModuleManager implements KotlinModuleManager {

    @NotNull
    public static IdeModuleManager getInstance(@NotNull Project project) {
        return ServiceManager.getService(project, IdeModuleManager.class);
    }

    private final Key<Collection<ModuleDescriptor>> KOTLIN_MODULES = Key.create("KOTLIN_MODULES");

    private final Project project;

    public IdeModuleManager(@NotNull Project project) {
        this.project = project;
    }

    @NotNull
    @Override
    public Collection<ModuleDescriptor> getModules() {
        return CachedValuesManager.getManager(project).getCachedValue(project, new CachedValueProvider<KotlinModules>() {
            @Nullable
            @Override
            public Result<KotlinModules> compute() {
                return Result.create(new ModuleBuilder().computeModules(), ProjectRootManager.getInstance(project));
            }
        }).getModuleDescriptors();
    }

    private class ModuleBuilder {

        private final Map<Module, ModuleDescriptor> kotlinModules = Maps.newHashMap();
        private final Map<Module, MutableSubModuleDescriptor> srcSubModules = Maps.newHashMap();
        private final Map<Module, MutableSubModuleDescriptor> testSubModules = Maps.newHashMap();
        private final Map<Library, MutableSubModuleDescriptor> librarySubModules = Maps.newHashMap();
        private final Map<Sdk, MutableSubModuleDescriptor> sdkSubModules = Maps.newHashMap();

        @NotNull
        public KotlinModules computeModules() {

            Module[] ideaModules = ModuleManager.getInstance(project).getModules();
            for (Module ideaModule : ideaModules) {
                createModuleAndSubModules(ideaModule);
            }

            for (Module ideaModule : ideaModules) {
                createDependencies(ideaModule);
            }

            return new KotlinModules(kotlinModules);
        }

        private void createModuleAndSubModules(Module ideaModule) {
            MutableModuleDescriptor moduleDescriptor = new MutableModuleDescriptor(
                    stringToSpecialName(ideaModule.getName()),
                    getClassMap(ideaModule)
            );
            kotlinModules.put(ideaModule, moduleDescriptor);

            Collection<SourceFolder> testFolders = Lists.newArrayList();
            Collection<SourceFolder> srcFolders = Lists.newArrayList();
            ModuleRootManager rootManager = ModuleRootManager.getInstance(ideaModule);
            for (ContentEntry contentEntry : rootManager.getContentEntries()) {
                for (SourceFolder sourceFolder : contentEntry.getSourceFolders()) {
                    if (sourceFolder.isTestSource()) {
                        testFolders.add(sourceFolder);
                    }
                    else {
                        srcFolders.add(sourceFolder);
                    }
                }
            }

            boolean hasSrc = !srcFolders.isEmpty();
            boolean hasTestSrc = !testFolders.isEmpty();

            MutableSubModuleDescriptor srcSubModule;
            if (hasSrc) {
                srcSubModule = new MutableSubModuleDescriptor(moduleDescriptor, Name.special("<production sources>"));

                moduleDescriptor.addSubModule(srcSubModule);

                srcSubModules.put(ideaModule, srcSubModule);
            }
            else {
                srcSubModule = null;
            }

            if (hasTestSrc) {
                MutableSubModuleDescriptor testSubModule = new MutableSubModuleDescriptor(moduleDescriptor, Name.special("<test sources>"));
                if (hasSrc) {
                    testSubModule.addDependency(srcSubModule);
                }

                moduleDescriptor.addSubModule(testSubModule);

                testSubModules.put(ideaModule, testSubModule);
            }

        }

        private void createDependencies(Module ideaModule) {
            final List<OrderEntry> sourceDependencies = Lists.newArrayList();
            final List<OrderEntry> testDependencies = Lists.newArrayList();

            OrderEnumerator.orderEntries(ideaModule).recursively().exportedOnly().forEach(
                    new Processor<OrderEntry>() {
                        @Override
                        public boolean process(OrderEntry entry) {
                            if (entry instanceof ExportableOrderEntry) {
                                DependencyScope scope = ((ExportableOrderEntry) entry).getScope();
                                if (scope == DependencyScope.RUNTIME) return true;
                                if (scope != DependencyScope.TEST) {
                                    sourceDependencies.add(entry);
                                }
                                testDependencies.add(entry);

                            }
                            else if (entry instanceof ModuleSourceOrderEntry) {
                                sourceDependencies.add(entry);
                                testDependencies.add(entry);
                            }
                            return true;
                        }
                    }
            );

            createDependenciesForSubModule(srcSubModules.get(ideaModule), sourceDependencies, false);
            createDependenciesForSubModule(testSubModules.get(ideaModule), testDependencies, true);
        }

        private void createDependenciesForSubModule(
                @Nullable MutableSubModuleDescriptor subModule,
                @NotNull List<OrderEntry> dependencies,
                boolean isTestSources
        ) {
            if (subModule == null) return;

            for (OrderEntry dependency : dependencies) {
                if (dependency instanceof ModuleOrderEntry) {
                    ModuleOrderEntry entry = (ModuleOrderEntry) dependency;
                    subModule.addDependency(srcSubModules.get(entry.getModule()));
                    if (isTestSources) {
                        subModule.addDependency(testSubModules.get(entry.getModule()));
                    }
                }
                else if (dependency instanceof ModuleSourceOrderEntry) {
                    subModule.addDependency(SubModuleDescriptor.MY_SOURCE);
                }
                else {
                    if (dependency instanceof LibraryOrderEntry) {
                        LibraryOrderEntry entry = (LibraryOrderEntry) dependency;

                        Library library = entry.getLibrary();
                        if (library == null) continue;

                        subModule.addDependency(
                                getCachedSubModule(
                                        librarySubModules,
                                        library,
                                        stringToSpecialName(library.getName()),
                                        library.getRootProvider()));
                    }
                    else if (dependency instanceof JdkOrderEntry) {
                        JdkOrderEntry entry = (JdkOrderEntry) dependency;

                        Sdk jdk = entry.getJdk();
                        if (jdk == null) continue;

                        subModule.addDependency(
                                getCachedSubModule(
                                        sdkSubModules,
                                        jdk,
                                        stringToSpecialName(jdk.getName()),
                                        jdk.getRootProvider()));
                    }
                }
            }
        }

        @NotNull
        private <K> SubModuleDescriptor getCachedSubModule(
                @NotNull Map<K, MutableSubModuleDescriptor> cache,
                @NotNull K key,
                @NotNull Name name,
                @NotNull RootProvider rootProvider
        ) {
            MutableSubModuleDescriptor subModule = cache.get(key);
            if (subModule == null) {
                List<VirtualFile> files = Arrays.asList(rootProvider.getFiles(OrderRootType.CLASSES));
                MutableModuleDescriptor moduleDescriptor = new MutableModuleDescriptor(name, JavaToKotlinClassMap.getInstance());
                subModule = new MutableSubModuleDescriptor(moduleDescriptor, name);
                cache.put(key, subModule);
            }
            return subModule;
        }

    }

    @NotNull
    private static PlatformToKotlinClassMap getClassMap(@NotNull Module ideaModule) {
        if (JsModuleDetector.isJsModule(ideaModule)) {
            return PlatformToKotlinClassMap.EMPTY;
        }
        else {
            return JavaToKotlinClassMap.getInstance();
        }
    }

    @NotNull
    private static Name stringToSpecialName(@Nullable String name) {
        return Name.special("<" + name + ">");
    }

    private static class KotlinModules {
        private final ImmutableMap<Module, ModuleDescriptor> moduleDescriptors;

        private KotlinModules(Map<Module, ModuleDescriptor> descriptors) {
            moduleDescriptors = ImmutableMap.copyOf(descriptors);
        }

        @NotNull
        public Collection<ModuleDescriptor> getModuleDescriptors() {
            //noinspection unchecked
            return (Collection) moduleDescriptors.values();
        }

    }
}
