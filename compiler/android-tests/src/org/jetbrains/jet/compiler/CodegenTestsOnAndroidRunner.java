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

package org.jetbrains.jet.compiler;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.util.io.FileUtil;
import junit.framework.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.compiler.ant.AntRunner;
import org.jetbrains.jet.compiler.download.SDKDownloader;
import org.jetbrains.jet.compiler.emulator.Emulator;
import org.jetbrains.jet.compiler.run.PermissionManager;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class CodegenTestsOnAndroidRunner {
    private static final Pattern ERROR_IN_TEST_OUTPUT_PATTERN =
            Pattern.compile("([\\s]+at .*| Caused .*| java.lang.RuntimeException: File: .*|[\\s]+\\.\\.\\. .* more| Error in .*)");
    private static final Pattern NUMBER_OF_TESTS_IF_FAILED = Pattern.compile("Tests run: ([0-9]*),  Failures: ([0-9]*),  Errors: ([0-9]*)");
    private static final Pattern NUMBER_OF_TESTS_OK = Pattern.compile(" OK \\(([0-9]*) tests\\)");

    private final PathManager pathManager;

    public static TestSuite getTestSuite(PathManager pathManager) {
        return new CodegenTestsOnAndroidRunner(pathManager).generateTestSuite();
    }

    private CodegenTestsOnAndroidRunner(PathManager pathManager) {
        this.pathManager = pathManager;
    }

    private TestSuite generateTestSuite() {
        TestSuite suite = new TestSuite("MySuite");

        String resultOutput = runTests();
        if (resultOutput == null) return suite;

        // Test name -> stackTrace
        final Map<String, String> resultMap = parseOutputForFailedTests(resultOutput);
        final Statistics statistics;

        // If map is empty => there are no failed tests
        if (resultMap.isEmpty()) {
            statistics = parseOutputForTestsNumberIfTestsPassed(resultOutput);
        }
        else {
            statistics = parseOutputForTestsNumberIfThereIsFailedTests(resultOutput);

            for (final Map.Entry<String, String> entry : resultMap.entrySet()) {

                suite.addTest(new TestCase("run") {
                    @Override
                    public String getName() {
                        return entry.getKey();
                    }

                    @Override
                    protected void runTest() throws Throwable {
                        Assert.fail(entry.getValue() + "See more information in log above.");
                    }
                });
            }
        }

        Assert.assertNotNull("Cannot parse number of failed tests from final line", statistics);
        Assert.assertEquals("Number of stackTraces != failed tests on the final line", resultMap.size(),
                            statistics.failed + statistics.errors);

        suite.addTest(new TestCase("run") {
            @Override
            public String getName() {
                return "testAll: Total: " + statistics.total + ", Failures: " + statistics.failed + ", Errors: " + statistics.errors;
            }

            @Override
            protected void runTest() throws Throwable {
                Assert.assertTrue(true);
            }
        });

        return suite;
    }


    /*
    Output example:
    [exec] Error in testKt344:
    [exec] java.lang.RuntimeException: File: compiler\testData\codegen\regressions\kt344.jet
    [exec] 	at org.jetbrains.jet.compiler.android.AbstractCodegenTestCaseOnAndroid.invokeBoxMethod(AbstractCodegenTestCaseOnAndroid.java:38)
    [exec] 	at org.jetbrains.jet.compiler.android.CodegenTestCaseOnAndroid.testKt344(CodegenTestCaseOnAndroid.java:595)
    [exec] 	at java.lang.reflect.Method.invokeNative(Native Method)
    [exec] 	at android.test.AndroidTestRunner.runTest(AndroidTestRunner.java:169)
    [exec] Caused by: java.lang.reflect.InvocationTargetException
    [exec] 	at java.lang.reflect.Method.invokeNative(Native Method)
    [exec] 	at org.jetbrains.jet.compiler.android.AbstractCodegenTestCaseOnAndroid.invokeBoxMethod(AbstractCodegenTestCaseOnAndroid.java:35)
    [exec] 	... 13 more
    [exec] Caused by: java.lang.VerifyError: compiler_testData_codegen_regressions_kt344_jet.namespace$t6$foo$1
    [exec] 	at compiler_testData_codegen_regressions_kt344_jet.namespace.t6(dummy.jet:94)
    [exec] 	at compiler_testData_codegen_regressions_kt344_jet.namespace.box(dummy.jet:185)
    [exec] 	... 16 more
    [exec] ...............
    [exec] Error in testKt529:
    */
    private Map<String, String> parseOutputForFailedTests(@NotNull String output) {
        Map<String, String> result = new HashMap<String, String>();
        StringBuilder builder = new StringBuilder();
        String failedTestNamePrefix = " Error in ";
        String lastFailedTestName = "";
        Matcher matcher = ERROR_IN_TEST_OUTPUT_PATTERN.matcher(output);
        while (matcher.find()) {
            String groupValue = matcher.group();
            if (groupValue.startsWith(failedTestNamePrefix)) {
                if (builder.length() > 0) {
                    result.put(lastFailedTestName, builder.toString());
                    builder.delete(0, builder.length());
                }
                lastFailedTestName = groupValue.substring(failedTestNamePrefix.length());
            }
            builder.append(groupValue);
            builder.append("\n");
        }
        if (builder.length() > 0) {
            result.put(lastFailedTestName, builder.toString());
        }
        return result;
    }

    //[exec] Tests run: 225,  Failures: 0,  Errors: 2
    @Nullable
    private Statistics parseOutputForTestsNumberIfThereIsFailedTests(String output) {
        Matcher matcher = NUMBER_OF_TESTS_IF_FAILED.matcher(output);
        if (matcher.find()) {
            return new Statistics(Integer.parseInt(matcher.group(1)), Integer.parseInt(matcher.group(2)),
                                  Integer.parseInt(matcher.group(3)));
        }
        return null;
    }

    //[exec] OK (223 tests)
    @Nullable
    private Statistics parseOutputForTestsNumberIfTestsPassed(String output) {
        Matcher matcher = NUMBER_OF_TESTS_OK.matcher(output);
        if (matcher.find()) {
            return new Statistics(Integer.parseInt(matcher.group(1)), 0, 0);
        }
        return null;
    }


    @Nullable
    public String runTests() {
        ApplicationManager.setApplication(null, new Disposable() {
            @Override
            public void dispose() {
            }
        });

        File rootForAndroidDependencies = new File(pathManager.getDependenciesRoot());
        if (!rootForAndroidDependencies.exists()) {
            rootForAndroidDependencies.mkdirs();
        }

        SDKDownloader downloader = new SDKDownloader(pathManager);
        Emulator emulator = new Emulator(pathManager);
        AntRunner antRunner = new AntRunner(pathManager);
        downloader.downloadAll();
        downloader.unzipAll();

        PermissionManager.setPermissions(pathManager);

        antRunner.packLibraries();

        try {
            emulator.createEmulator();
            emulator.startEmulator();

            try {
                emulator.waitEmulatorStart();
                antRunner.cleanOutput();
                antRunner.compileSources();
                antRunner.installApplicationOnEmulator();
                return antRunner.runTestsOnEmulator();
            }
            catch (RuntimeException e) {
                e.printStackTrace();
                throw e;
            }
            finally {
                emulator.stopEmulator();
            }
        }
        catch (RuntimeException e) {
            e.printStackTrace();
            throw e;
        }
        finally {
            emulator.finishEmulatorProcesses();
        }
    }

    private static class Statistics {
        public final int total;
        public final int errors;
        public final int failed;

        private Statistics(int total, int failed, int errors) {
            this.total = total;
            this.failed = failed;
            this.errors = errors;
        }
    }
}
