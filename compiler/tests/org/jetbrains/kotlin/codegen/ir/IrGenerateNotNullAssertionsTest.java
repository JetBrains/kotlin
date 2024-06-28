/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.codegen.ir;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.codegen.GenerateNotNullAssertionsTest;
import org.jetbrains.kotlin.test.TargetBackend;

@SuppressWarnings("JUnitTestCaseWithNoTests")
public class IrGenerateNotNullAssertionsTest extends GenerateNotNullAssertionsTest {
    @NotNull
    @Override
    public TargetBackend getBackend() {
        return TargetBackend.JVM_IR;
    }
}
