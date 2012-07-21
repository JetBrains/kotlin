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

import com.intellij.openapi.roots.libraries.LibraryProperties;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author Maxim.Manuylov
 *         Date: 19.05.12
 */
public class KotlinSdkProperties extends LibraryProperties<KotlinSdkProperties> {
    @NotNull private String myVersion;

    public KotlinSdkProperties(@NotNull final String version) {
        myVersion = version;
    }

    @NotNull
    public String getVersion() {
        return myVersion;
    }

    @Override
    public KotlinSdkProperties getState() {
        return this;
    }

    @Override
    public void loadState(@NotNull final KotlinSdkProperties state) {
        myVersion = state.myVersion;
    }

    @Override
    public boolean equals(@Nullable final Object that) {
        return this == that || (that instanceof KotlinSdkProperties && myVersion.equals(((KotlinSdkProperties)that).myVersion));
    }

    @Override
    public int hashCode() {
        return myVersion.hashCode();
    }
}
