/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.android.tests;

import com.intellij.util.PlatformUtils;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.android.tests.download.SDKDownloader;
import org.jetbrains.kotlin.android.tests.emulator.Emulator;
import org.jetbrains.kotlin.android.tests.gradle.GradleRunner;
import org.jetbrains.kotlin.android.tests.run.PermissionManager;
import org.junit.Assert;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class CodegenTestsOnAndroidRunner {

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

        String reportFolder = pathManager.getTmpFolder() + "/build/outputs/androidTest-results/connected";
        try {
            List<TestCase> testCases = parseSingleReportInFolder(reportFolder);
            for (TestCase aCase : testCases) {
                suite.addTest(aCase);
            }
            Assert.assertNotEquals("There is no test results in report", 0, testCases.size());
        }
        catch (Exception e) {
            throw new RuntimeException("Can't parse test results in " + reportFolder +"\n" + resultOutput);
        }

        return suite;
    }

    @Nullable
    public String runTests() {
        File rootForAndroidDependencies = new File(pathManager.getDependenciesRoot());
        if (!rootForAndroidDependencies.exists()) {
            rootForAndroidDependencies.mkdirs();
        }

        SDKDownloader downloader = new SDKDownloader(pathManager);
        downloader.downloadAll();
        downloader.unzipAll();
        PermissionManager.setPermissions(pathManager);

        Emulator emulator = new Emulator(pathManager, Emulator.ARM);
        GradleRunner gradleRunner = new GradleRunner(pathManager);
        gradleRunner.clean();
        gradleRunner.build();

        emulator.createEmulator();

        String platformPrefixProperty = System.setProperty(PlatformUtils.PLATFORM_PREFIX_KEY, "Idea");
        try {
            emulator.startEmulator();

            try {
                emulator.waitEmulatorStart();
                //runTestsViaAdb(emulator, gradleRunner);
                return gradleRunner.connectedDebugAndroidTest();
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
            if (platformPrefixProperty != null) {
                System.setProperty(PlatformUtils.PLATFORM_PREFIX_KEY, platformPrefixProperty);
            }
            else {
                System.clearProperty(PlatformUtils.PLATFORM_PREFIX_KEY);
            }
            emulator.finishEmulatorProcesses();
        }
    }

    private String runTestsViaAdb(Emulator emulator, GradleRunner gradleRunner) {
        gradleRunner.installDebugAndroidTest();
        String result = emulator.runTestsViaAdb();
        System.out.println(result);
        gradleRunner.uninstallDebugAndroidTest();
        return result;
    }

    private static List<TestCase> parseSingleReportInFolder(String reportFolder) throws
                                                                                 IOException,
                                                                                 SAXException,
                                                                                 ParserConfigurationException {
        File folder = new File(reportFolder);
        File[] files = folder.listFiles();
        assert files != null;
        assert files.length == 1;
        File reportFile = files[0];

        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
        Document doc = dBuilder.parse(reportFile);
        Element root = doc.getDocumentElement();
        NodeList testCases = root.getElementsByTagName("testcase");
        List<TestCase> result = new ArrayList(testCases.getLength());

        for (int i = 0; i < testCases.getLength(); i++) {
            Element item = (Element) testCases.item(i);
            NodeList failure = item.getElementsByTagName("failure");
            String name = item.getAttribute("name");
            String clazz = item.getAttribute("classname");

            if (failure.getLength() == 0) {
                result.add(new TestCase(name) {
                    @Override
                    protected void runTest() throws Throwable {

                    }
                });
            }
            else {
                result.add(new TestCase(name) {
                    @Override
                    protected void runTest() throws Throwable {
                        Assert.fail(failure.item(0).getTextContent());
                    }
                });
            }
        }

        return result;
    }
}
