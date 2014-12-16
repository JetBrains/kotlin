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
import org.jetbrains.jet.lang.resolve.kotlin.JavaDeclarationCheckerProvider;
import org.jetbrains.jet.lang.types.DynamicTypesAllowed;
import org.jetbrains.jet.lang.types.DynamicTypesSettings;
import org.jetbrains.k2js.resolve.KotlinJsDeclarationCheckerProvider;

public interface TargetPlatform {
    @NotNull
    AdditionalCheckerProvider getAdditionalCheckerProvider();

    @NotNull
    DynamicTypesSettings getDynamicTypesSettings();

    TargetPlatform JVM = new TargetPlatformImpl("JVM", JavaDeclarationCheckerProvider.INSTANCE$, new DynamicTypesSettings());
    TargetPlatform JS = new TargetPlatformImpl("JS", KotlinJsDeclarationCheckerProvider.INSTANCE$, new DynamicTypesAllowed());
}
