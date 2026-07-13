/*
 * Copyright 2000-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.codegen;

import kotlin.text.StringsKt;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.descriptors.ModuleDescriptor;
import org.jetbrains.kotlin.metadata.jvm.deserialization.ModuleMapping;
import org.jetbrains.kotlin.name.Name;

public class ModuleNameUtil {
    private ModuleNameUtil() {
    }

    @NotNull
    public static String getModuleName(ModuleDescriptor module) {
        Name stableName = module.getStableName();
        if (stableName == null) {
            // Defensive fallback to possibly unstable name, to not fail with exception
            return prepareModuleName(module.getName());
        } else {
            return prepareModuleName(stableName);
        }
    }

    @NotNull
    public static String prepareModuleName(@NotNull Name name) {
        return StringsKt.removeSurrounding(name.asString(), "<", ">");
    }

    @NotNull
    public static String getMappingFileName(@NotNull String moduleName) {
        // Replace characters forbidden in Windows file names: < > : " / \ | ? *
        // and ASCII control characters 0x00–0x1F, replacing each with '_'.
        // '%' is also replaced to avoid variable expansion in shell/batch environments.
        String sanitizedName = moduleName.replaceAll("[<>:\"/\\\\|?*%\\x00-\\x1F]", "_");
        return "META-INF/" + sanitizedName + "." + ModuleMapping.MAPPING_FILE_EXT;
    }
}
