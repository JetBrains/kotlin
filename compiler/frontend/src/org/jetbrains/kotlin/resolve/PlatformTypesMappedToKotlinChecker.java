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

package org.jetbrains.kotlin.resolve;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.descriptors.ClassDescriptor;
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor;
import org.jetbrains.kotlin.descriptors.ModuleDescriptor;
import org.jetbrains.kotlin.platform.PlatformToKotlinClassMap;
import org.jetbrains.kotlin.psi.JetElement;
import org.jetbrains.kotlin.psi.JetExpression;
import org.jetbrains.kotlin.psi.JetImportDirective;

import java.util.Collection;

import static org.jetbrains.kotlin.diagnostics.Errors.PLATFORM_CLASS_MAPPED_TO_KOTLIN;

public class PlatformTypesMappedToKotlinChecker {

    public static void checkPlatformTypesMappedToKotlin(
            @NotNull ModuleDescriptor module,
            @NotNull BindingTrace trace,
            @NotNull JetImportDirective importDirective,
            @NotNull Collection<? extends DeclarationDescriptor> descriptors
    ) {
        JetExpression importedReference = importDirective.getImportedReference();
        if (importedReference != null) {
            for (DeclarationDescriptor descriptor : descriptors) {
                reportPlatformClassMappedToKotlin(module, trace, importedReference, descriptor);
            }
        }
    }

    public static void reportPlatformClassMappedToKotlin(
            @NotNull ModuleDescriptor module,
            @NotNull BindingTrace trace,
            @NotNull JetElement element,
            @NotNull DeclarationDescriptor descriptor
    ) {
        if (!(descriptor instanceof ClassDescriptor)) return;

        PlatformToKotlinClassMap platformToKotlinMap = module.getPlatformToKotlinClassMap();
        Collection<ClassDescriptor> kotlinAnalogsForClass = platformToKotlinMap.mapPlatformClass((ClassDescriptor) descriptor);
        if (!kotlinAnalogsForClass.isEmpty()) {
            trace.report(PLATFORM_CLASS_MAPPED_TO_KOTLIN.on(element, kotlinAnalogsForClass));
        }
    }
}
