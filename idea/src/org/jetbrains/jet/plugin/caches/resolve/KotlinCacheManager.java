/*
 * Copyright 2010-2014 JetBrains s.r.o.
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

package org.jetbrains.jet.plugin.caches.resolve;

import com.google.common.collect.Maps;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.DefaultModificationTracker;
import com.intellij.openapi.util.ModificationTracker;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.plugin.project.TargetPlatform;

import java.util.Map;

public class KotlinCacheManager {
    @NotNull
    public static KotlinCacheManager getInstance(@NotNull Project project) {
        return ServiceManager.getService(project, KotlinCacheManager.class);
    }

    private final Map<TargetPlatform, DeclarationsCacheProvider> cacheProviders = Maps.newHashMap();

    private final DefaultModificationTracker kotlinDeclarationsTracker = new DefaultModificationTracker();

    public KotlinCacheManager(@NotNull Project project) {
        cacheProviders.put(TargetPlatform.JVM, new JvmDeclarationsCacheProvider(project));
        cacheProviders.put(TargetPlatform.JS, new JSDeclarationsCacheProvider(project));
    }

    @NotNull
    public DeclarationsCacheProvider getRegisteredProvider(@NotNull TargetPlatform platform) {
        DeclarationsCacheProvider provider = cacheProviders.get(platform);
        if (provider == null) {
            throw new IllegalStateException("Provider isn't registered for platform: " + platform);
        }

        return provider;
    }

    public ModificationTracker getDeclarationsTracker() {
        return kotlinDeclarationsTracker;
    }
}
