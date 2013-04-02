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

package org.jetbrains.jet.plugin.caches.resolve;

import com.google.common.collect.Maps;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.plugin.project.TargetPlatform;

import java.util.Map;

public class KotlinCacheManager {
    public static KotlinCacheManager getInstance(@NotNull Project project) {
        return ServiceManager.getService(project, KotlinCacheManager.class);
    }

    private final Map<TargetPlatform, DeclarationsCacheProvider> cacheProviders = Maps.newHashMap();

    public KotlinCacheManager(@NotNull Project project) {
        cacheProviders.put(TargetPlatform.JVM, new JvmDeclarationsCacheProvider(project));
        cacheProviders.put(TargetPlatform.JS, new JSDeclarationsCacheProvider(project));
    }

    /**
     * Should be called under read lock.
     */
    @NotNull
    public KotlinDeclarationsCache getDeclarationsFromProject(@NotNull TargetPlatform platform) {
        // Computing declarations should be performed under read lock
        ApplicationManager.getApplication().assertReadAccessAllowed();
        return getRegisteredProvider(platform).getDeclarations(false);
    }

    @NotNull
    public KotlinDeclarationsCache getPossiblyIncompleteDeclarationsForLightClassGeneration() {
        // Computing declarations should be performed under read lock
        ApplicationManager.getApplication().assertReadAccessAllowed();

        /*
         * If we have the following classes
         *
         *     class A // Kotlin
         *     class B extends A {} // Java
         *     class C : B() // Kotlin
         *
         *  The analysis runs into infinite recursion, because
         *      C needs all members of B (to compute overrides),
         *      and B needs all members of A,
         *      and A is not available from KotlinCacheManager.getDeclarationsFromProject() -- it is being computed right now,
         *      so the analysis runs again...
         *
         *  Our workaround is to return partially complete results when we generate light classes
         */
        return getRegisteredProvider(TargetPlatform.JVM).getDeclarations(true);
    }

    @NotNull
    private DeclarationsCacheProvider getRegisteredProvider(TargetPlatform platform) {
        DeclarationsCacheProvider provider = cacheProviders.get(platform);
        if (provider == null) {
            throw new IllegalStateException("Provider isn't registered for platform: " + platform);
        }

        return provider;
    }
}
