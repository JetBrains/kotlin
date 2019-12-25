/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.checkers;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.test.ConfigurationKind;

public abstract class AbstractDiagnosticsTestWithStdLib extends AbstractDiagnosticsTest {
    @NotNull
    @Override
    protected ConfigurationKind getConfigurationKind() {
        return ConfigurationKind.NO_KOTLIN_REFLECT;
    }
}
