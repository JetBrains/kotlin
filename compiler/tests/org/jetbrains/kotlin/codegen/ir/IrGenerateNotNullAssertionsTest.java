/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.codegen.ir;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.codegen.AbstractGenerateNotNullAssertionsTest;
import org.jetbrains.kotlin.config.CompilerConfiguration;
import org.jetbrains.kotlin.config.JVMConfigurationKeys;

public class IrGenerateNotNullAssertionsTest extends AbstractGenerateNotNullAssertionsTest {
    @Override
    public void updateConfiguration(@NotNull CompilerConfiguration configuration) {
        configuration.put(JVMConfigurationKeys.IR, true);
    }

    public void testNoAssertionsForKotlinFromBinary() {
        doTestNoAssertionsForKotlinFromBinary("noAssertionsForKotlin.kt", "noAssertionsForKotlinMain.kt");
    }
}
