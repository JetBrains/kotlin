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

package org.jetbrains.jet.plugin.framework;

import com.intellij.openapi.roots.OrderRootType;
import com.intellij.openapi.roots.libraries.Library;
import com.intellij.openapi.roots.libraries.LibraryPresentationProvider;
import com.intellij.openapi.roots.libraries.LibraryProperties;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;

public class LibraryPresentationProviderUtil {
    private LibraryPresentationProviderUtil() {}

    public static <LP extends LibraryProperties> boolean isDetected(@NotNull LibraryPresentationProvider<LP> provider, @NotNull Library library) {
        return getLibraryProperties(provider, library) != null;
    }

    @Nullable
    public static <LP extends LibraryProperties> LP getLibraryProperties(@NotNull LibraryPresentationProvider<LP> provider, @NotNull Library library) {
        return provider.detect(Arrays.asList(library.getFiles(OrderRootType.CLASSES)));
    }
}
