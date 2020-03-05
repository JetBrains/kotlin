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

    // True if the latest stable language version supported by this compiler has not yet been released.
    // Binaries produced by this compiler with that language version (or any future language version) are going to be marked
    // as "pre-release" and will not be loaded by release versions of the compiler.
    // Change this value before and after every major release
    private static final boolean IS_PRE_RELEASE = true;

    public static final String TEST_IS_PRE_RELEASE_SYSTEM_PROPERTY = "kotlin.test.is.pre.release";

    public static boolean isPreRelease() {
        String overridden = System.getProperty(TEST_IS_PRE_RELEASE_SYSTEM_PROPERTY);
        if (overridden != null) {
            return Boolean.parseBoolean(overridden);
        }

        return IS_PRE_RELEASE;
    }

    /**
     * @return version of this compiler, or `null` if it isn't known (if VERSION is "@snapshot@")
     */
    @Nullable
    public static String getVersion() {
        return VERSION.equals("@snapshot@") ? null : VERSION;
    }

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

        if (!VERSION.equals("@snapshot@") && !VERSION.contains("-") && IS_PRE_RELEASE) {
            throw new IllegalStateException(
                    "IS_PRE_RELEASE cannot be true for a compiler without '-' in its version.\n" +
                    "Please change IS_PRE_RELEASE to false, commit and push this change to master"
            );
        }
    }
}
