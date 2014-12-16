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

package org.jetbrains.jet.plugin.project;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.resolve.AdditionalCheckerProvider;
import org.jetbrains.jet.lang.types.DynamicTypesSettings;

public class TargetPlatformImpl implements TargetPlatform {
    @NotNull private final String platformName;
    @NotNull private final AdditionalCheckerProvider additionalCheckerProvider;
    @NotNull private final DynamicTypesSettings dynamicTypesSettings;

    public TargetPlatformImpl(
            @NotNull String platformName,
            @NotNull AdditionalCheckerProvider additionalCheckerProvider,
            @NotNull DynamicTypesSettings dynamicTypesSettings
    ) {
        this.platformName = platformName;
        this.additionalCheckerProvider = additionalCheckerProvider;
        this.dynamicTypesSettings = dynamicTypesSettings;
    }

    @NotNull
    @Override
    public AdditionalCheckerProvider getAdditionalCheckerProvider() {
        return additionalCheckerProvider;
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
