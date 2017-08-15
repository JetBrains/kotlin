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

import com.intellij.openapi.module.Module;
import com.intellij.openapi.roots.ProjectRootModificationTracker;
import com.intellij.openapi.util.Key;
import com.intellij.psi.util.CachedValue;
import com.intellij.psi.util.CachedValueProvider;
import com.intellij.psi.util.CachedValuesManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.caches.resolve.IdePlatformSupport;
import org.jetbrains.kotlin.resolve.TargetPlatform;

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
                    return Result.create(IdePlatformSupport.getPlatformForModule(module),
                                         ProjectRootModificationTracker.getInstance(module.getProject()));
                }
            }, false);

            module.putUserData(PLATFORM_FOR_MODULE, result);
        }

        return result.getValue();
    }
}
