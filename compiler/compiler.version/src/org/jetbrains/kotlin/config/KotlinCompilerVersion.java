/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.config;

import org.jetbrains.annotations.Nullable;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class KotlinCompilerVersion {
    public static final String VERSION_FILE_PATH = "/META-INF/compiler.version";
    public static final String VERSION;

    /**
     * @return version of this compiler, or `null` if it isn't known (if VERSION is "@snapshot@")
     */
    @Nullable
    public static String getVersion() {
        return VERSION.equals("@snapshot@") ? null : VERSION;
    }

    @SuppressWarnings({"TryFinallyCanBeTryWithResources", "ConstantConditions"})
    private static String loadKotlinCompilerVersion() throws IOException {
        BufferedReader versionReader = new BufferedReader(
                new InputStreamReader(KotlinCompilerVersion.class.getResourceAsStream(VERSION_FILE_PATH)));
        try {
            return versionReader.readLine();
        } finally {
            versionReader.close();
        }
    }

    static {
        try {
            VERSION = loadKotlinCompilerVersion();
        }
        catch (IOException e) {
            throw new IllegalStateException("Failed to read compiler version from " + VERSION_FILE_PATH);
        }
    }
}
