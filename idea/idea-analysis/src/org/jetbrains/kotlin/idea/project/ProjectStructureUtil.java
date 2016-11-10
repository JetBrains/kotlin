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
import org.jetbrains.kotlin.idea.framework.JsLibraryStdDetectionUtil;

public class ProjectStructureUtil {
    private static final Key<CachedValue<Boolean>> IS_KOTLIN_JS_MODULE = Key.create("IS_KOTLIN_JS_MODULE");

    private ProjectStructureUtil() {
    }

    public static boolean isJsKotlinModule(@NotNull final Module module) {
        CachedValue<Boolean> result = module.getUserData(IS_KOTLIN_JS_MODULE);
        if (result == null) {
            result = CachedValuesManager.getManager(module.getProject()).createCachedValue(new CachedValueProvider<Boolean>() {
                @Override
                public Result<Boolean> compute() {
                    return Result.create(
                            getJSStandardLibrary(module) != null,
                            ProjectRootModificationTracker.getInstance(module.getProject()));
                }
            }, false);

            module.putUserData(IS_KOTLIN_JS_MODULE, result);
        }

        return result.getValue();
    }

    @Nullable
    private static Library getJSStandardLibrary(final Module module) {
        return ApplicationManager.getApplication().runReadAction(new Computable<Library>() {
            @Override
            public Library compute() {
                final Ref<Library> jsLibrary = Ref.create();

                ModuleRootManager.getInstance(module).orderEntries().librariesOnly().forEachLibrary(new Processor<Library>() {
                    @Override
                    public boolean process(Library library) {
                        if (JsLibraryStdDetectionUtil.hasJsStdlibJar(library)) {
                            jsLibrary.set(library);
                            return false;
                        }

                        return true;
                    }
                });

                return jsLibrary.get();
            }
        });
    }
}
