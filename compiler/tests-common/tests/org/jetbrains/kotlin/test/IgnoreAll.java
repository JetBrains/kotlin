/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test;

import junit.framework.Test;
import junit.framework.TestResult;
import org.junit.internal.runners.JUnit38ClassRunner;

public class IgnoreAll extends JUnit38ClassRunner {
    public IgnoreAll(@SuppressWarnings("unused") Class<?> klass) {
        super(new Test() {
            @Override
            public int countTestCases() {
                return 0;
            }

            @Override
            public void run(TestResult result) {
                result.startTest(this);
                result.endTest(this);
            }
        });
    }
}
