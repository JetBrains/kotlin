/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.jvm.compiler;

import org.jetbrains.kotlin.checkers.CompilerTestLanguageVersionSettings;
import org.jetbrains.kotlin.cli.jvm.config.JvmContentRootsKt;
import org.jetbrains.kotlin.codegen.forTestCompile.ForTestCompileRuntime;
import org.jetbrains.kotlin.config.*;
import org.jetbrains.kotlin.test.Directives;
import org.jetbrains.kotlin.test.InTextDirectivesUtils;
import org.jetbrains.kotlin.test.KotlinTestUtils;
import org.jetbrains.kotlin.test.util.RecursiveDescriptorComparator.Configuration;

import java.util.Collections;

import static org.jetbrains.kotlin.checkers.CompilerTestLanguageVersionSettingsKt.parseLanguageVersionSettings;
import static org.jetbrains.kotlin.test.util.RecursiveDescriptorComparator.DONT_INCLUDE_METHODS_OF_OBJECT;

public abstract class AbstractLoadJavaTest {
    // There are two modules in each test case (sources and dependencies), so we should render declarations from both of them
    public static final Configuration
            COMPARATOR_CONFIGURATION = DONT_INCLUDE_METHODS_OF_OBJECT.renderDeclarationsFromOtherModules(true);

    public static void updateConfigurationWithDirectives(String content, CompilerConfiguration configuration) {
        Directives directives = KotlinTestUtils.parseDirectives(content);
        LanguageVersionSettings languageVersionSettings = parseLanguageVersionSettings(directives, LanguageVersion.KOTLIN_1_9);
        if (languageVersionSettings == null) {
            languageVersionSettings = new CompilerTestLanguageVersionSettings(
                    Collections.emptyMap(), ApiVersion.KOTLIN_1_9, LanguageVersion.KOTLIN_1_9,
                    Collections.emptyMap()
            );
        }

        CommonConfigurationKeysKt.setLanguageVersionSettings(configuration, languageVersionSettings);

        if (InTextDirectivesUtils.isDirectiveDefined(content, "WITH_KOTLIN_JVM_ANNOTATIONS")) {
            JvmContentRootsKt.addJvmClasspathRoot(configuration, ForTestCompileRuntime.jvmAnnotationsForTests());
        }

        if (InTextDirectivesUtils.isDirectiveDefined(content, "USE_TYPE_TABLE")) {
            configuration.put(JVMConfigurationKeys.USE_TYPE_TABLE, true);
        }
    }
}
