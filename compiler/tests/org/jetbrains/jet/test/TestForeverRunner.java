/*
 * Copyright 2010-2013 JetBrains s.r.o.
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

package org.jetbrains.jet.test;

import com.google.common.base.Supplier;
import junit.framework.Test;
import junit.framework.TestResult;
import junit.textui.ResultPrinter;
import junit.textui.TestRunner;
import org.junit.Assert;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

public class TestForeverRunner {
    public static void runTestForever(String[] args, Supplier<Test> suite) {
        Assert.assertTrue(TestRunner.run(suite.get()).wasSuccessful());
        Assert.assertTrue(TestRunner.run(suite.get()).wasSuccessful());

        int limit = args.length > 0 ? Integer.parseInt(args[0]) : Integer.MAX_VALUE;

        long total = 0;
        long min = Long.MAX_VALUE;
        long[] last100 = new long[100];
        long[] last10 = new long[10];
        for (int i = -100; i < limit; ++i) {
            TestRunner runner = new TestRunner();
            runner.setPrinter(new ResultPrinter(new PrintStream(new ByteArrayOutputStream())));
            long start = System.nanoTime();
            TestResult result = runner.doRun(suite.get());
            Assert.assertTrue(result.wasSuccessful());
            long d = System.nanoTime() - start;
            last100[(i + 100) % 100] = d;
            last10[(i + 100) % 10] = d;
            long dMs = d / 1000 / 1000;
            if (i >= 1) {
                total += d;
                min = Math.min(min, d);
                long avg = total / i / 1000 / 1000;
                long avg10 = avg(last10) / 1000 / 1000;
                long avg100 = avg(last100) / 1000 / 1000;
                long minMs = min / 1000 / 1000;
                System.out.println(dMs + "ms; avg=" + avg + "ms; avg10=" + avg10 + "ms; avg100=" + avg100 + "ms; min=" + minMs + "ms; iteration=" + i);
            }
            else {
                System.out.println(dMs + "ms; iteration=" + i);
            }
        }
    }

    private static long avg(long[] array) {
        long sum = 0;
        for (long l : array) {
            sum += l;
        }
        return sum / array.length;
    }
}
