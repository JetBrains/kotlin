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

import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.types.DynamicTypesSettings;

public abstract class TargetPlatformImpl implements TargetPlatform {
    private final String platformName;
    private final DynamicTypesSettings dynamicTypesSettings;

    public TargetPlatformImpl(@NotNull String platformName, @NotNull DynamicTypesSettings dynamicTypesSettings) {
        this.platformName = platformName;
        this.dynamicTypesSettings = dynamicTypesSettings;
    }

    @NotNull
    @Override
    public DynamicTypesSettings getDynamicTypesSettings() {
        return dynamicTypesSettings;
    }

    @Override
    public String toString() {
        return platformName;
    }
}
