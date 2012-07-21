/*
 * Copyright 2010-2012 JetBrains s.r.o.
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

package org.jetbrains.jet.plugin.sdk;

import com.intellij.openapi.roots.libraries.LibraryPresentationProvider;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.plugin.JetIcons;

import javax.swing.*;
import java.util.List;

/**
 * @author Maxim.Manuylov
 *         Date: 19.05.12
 */
public class KotlinSdkPresentationProvider extends LibraryPresentationProvider<KotlinSdkProperties> {
    public KotlinSdkPresentationProvider() {
        super(KotlinSdkUtil.getKotlinSdkLibraryKind());
    }

    @NotNull
    @Override
    public Icon getIcon() {
        return JetIcons.SMALL_LOGO;
    }

    @NotNull
    @Override
    public String getDescription(@NotNull final KotlinSdkProperties properties) {
        return KotlinSdkUtil.getSDKName(properties.getVersion());
    }

    @Nullable
    @Override
    public KotlinSdkProperties detect(@NotNull final List<VirtualFile> classesRoots) {
        final String sdkVersion = KotlinSdkUtil.detectSDKVersion(classesRoots);
        return sdkVersion == null ? null : new KotlinSdkProperties(sdkVersion);
    }
}
