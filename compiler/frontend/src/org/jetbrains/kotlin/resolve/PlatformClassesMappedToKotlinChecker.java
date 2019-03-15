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
import org.jetbrains.kotlin.builtins.PlatformToKotlinClassMap;
import org.jetbrains.kotlin.descriptors.ClassDescriptor;
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor;
import org.jetbrains.kotlin.psi.KtElement;
import org.jetbrains.kotlin.psi.KtExpression;
import org.jetbrains.kotlin.psi.KtImportDirective;

import java.util.Collection;

import static org.jetbrains.kotlin.diagnostics.Errors.PLATFORM_CLASS_MAPPED_TO_KOTLIN;

public class PlatformClassesMappedToKotlinChecker {
    public static void checkPlatformClassesMappedToKotlin(
            @NotNull PlatformToKotlinClassMap platformToKotlinMap,
            @NotNull BindingTrace trace,
            @NotNull KtImportDirective importDirective,
            @NotNull Collection<? extends DeclarationDescriptor> descriptors
    ) {
        KtExpression importedReference = importDirective.getImportedReference();
        if (importedReference != null) {
            for (DeclarationDescriptor descriptor : descriptors) {
                reportPlatformClassMappedToKotlin(platformToKotlinMap, trace, importedReference, descriptor);
            }
        }
    }

    public static void reportPlatformClassMappedToKotlin(
            @NotNull PlatformToKotlinClassMap platformToKotlinMap,
            @NotNull BindingTrace trace,
            @NotNull KtElement element,
            @NotNull DeclarationDescriptor descriptor
    ) {
        if (!(descriptor instanceof ClassDescriptor)) return;

        Collection<ClassDescriptor> kotlinAnalogsForClass = platformToKotlinMap.mapPlatformClass((ClassDescriptor) descriptor);
        if (!kotlinAnalogsForClass.isEmpty()) {
            trace.report(PLATFORM_CLASS_MAPPED_TO_KOTLIN.on(element, kotlinAnalogsForClass));
        }
    }
}
