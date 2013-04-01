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

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.psi.JetElement;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.plugin.project.TargetPlatformDetector;

public class KotlinCacheManagerUtil {
    private KotlinCacheManagerUtil() {
    }

    @NotNull
    public static KotlinDeclarationsCache getDeclarationsFromProject(@NotNull JetElement element) {
        JetFile jetFile = (JetFile) element.getContainingFile();
        return KotlinCacheManager.getInstance(jetFile.getProject()).getDeclarationsFromProject(TargetPlatformDetector.getPlatform(jetFile));
    }

    @NotNull
    public static BindingContext getDeclarationsBindingContext(@NotNull JetElement element) {
        JetFile jetFile = (JetFile) element.getContainingFile();
        KotlinDeclarationsCache declarationsCache = KotlinCacheManager.getInstance(jetFile.getProject())
                .getDeclarationsFromProject(TargetPlatformDetector.getPlatform(jetFile));
        return declarationsCache.getBindingContext();
    }
}
