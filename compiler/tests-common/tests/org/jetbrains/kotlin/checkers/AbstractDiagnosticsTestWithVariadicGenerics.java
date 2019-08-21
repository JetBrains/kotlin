/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.checkers;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.cli.jvm.config.JvmContentRootsKt;
import org.jetbrains.kotlin.config.CompilerConfiguration;
import org.jetbrains.kotlin.test.ConfigurationKind;
import org.jetbrains.kotlin.utils.PathUtil;

public class AbstractDiagnosticsTestWithVariadicGenerics extends AbstractDiagnosticsTest {

    @NotNull
    @Override
    protected ConfigurationKind getConfigurationKind() {
        return ConfigurationKind.NO_KOTLIN_REFLECT;
    }

    @Override
    protected void performCustomConfiguration(@NotNull CompilerConfiguration configuration) {
        JvmContentRootsKt.addJvmClasspathRoot(configuration, PathUtil.getKotlinPathsForDistDirectory().getVariadicGenericsPath());
    }
}
