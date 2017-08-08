/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.idea.project;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.roots.ProjectRootModificationTracker;
import com.intellij.openapi.roots.libraries.Library;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.Ref;
import com.intellij.psi.util.CachedValue;
import com.intellij.psi.util.CachedValueProvider;
import com.intellij.psi.util.CachedValuesManager;
import com.intellij.util.Processor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.config.KotlinFacetSettings;
import org.jetbrains.kotlin.config.KotlinFacetSettingsProvider;
import org.jetbrains.kotlin.config.TargetPlatformKind;
import org.jetbrains.kotlin.idea.framework.JsLibraryStdDetectionUtil;
import org.jetbrains.kotlin.js.resolve.JsPlatform;
import org.jetbrains.kotlin.resolve.TargetPlatform;
import org.jetbrains.kotlin.resolve.jvm.platform.JvmPlatform;

public class ProjectStructureUtil {
    private static final Key<CachedValue<TargetPlatform>> PLATFORM_FOR_MODULE = Key.create("PLATFORM_FOR_MODULE");

    private ProjectStructureUtil() {
    }

    @NotNull
    /* package */ static TargetPlatform getCachedPlatformForModule(@NotNull final Module module) {
        CachedValue<TargetPlatform> result = module.getUserData(PLATFORM_FOR_MODULE);
        if (result == null) {
            result = CachedValuesManager.getManager(module.getProject()).createCachedValue(new CachedValueProvider<TargetPlatform>() {
                @Override
                public Result<TargetPlatform> compute() {
                    TargetPlatform configuredInFacet = getPlatformConfiguredInFacet(module);
                    TargetPlatform platform =
                            configuredInFacet != null ? configuredInFacet :
                            hasJsStandardLibraryInDependencies(module) ? JsPlatform.INSTANCE : JvmPlatform.INSTANCE;
                    return Result.create(platform, ProjectRootModificationTracker.getInstance(module.getProject()));
                }
            }, false);

            module.putUserData(PLATFORM_FOR_MODULE, result);
        }

        return result.getValue();
    }

    @Nullable
    private static TargetPlatform getPlatformConfiguredInFacet(@NotNull Module module) {
        KotlinFacetSettings settings = KotlinFacetSettingsProvider.Companion.getInstance(module.getProject()).getInitializedSettings(module);
        TargetPlatformKind<?> kind = settings.getTargetPlatformKind();
        if (kind instanceof TargetPlatformKind.Jvm) {
            return JvmPlatform.INSTANCE;
        }
        if (kind instanceof TargetPlatformKind.JavaScript) {
            return JsPlatform.INSTANCE;
        }
        if (kind instanceof TargetPlatformKind.Common) {
            return TargetPlatform.Default.INSTANCE;
        }
        return null;
    }

    private static boolean hasJsStandardLibraryInDependencies(@NotNull final Module module) {
        return ApplicationManager.getApplication().runReadAction(new Computable<Boolean>() {
            @Override
            public Boolean compute() {
                final Ref<Library> jsLibrary = Ref.create();

                ModuleRootManager.getInstance(module).orderEntries().librariesOnly().forEachLibrary(new Processor<Library>() {
                    @Override
                    public boolean process(Library library) {
                        if (JsLibraryStdDetectionUtil.INSTANCE.hasJsStdlibJar(library, false)) {
                            jsLibrary.set(library);
                            return false;
                        }

                        return true;
                    }
                });

                return jsLibrary.get() != null;
            }
        });
    }
}
