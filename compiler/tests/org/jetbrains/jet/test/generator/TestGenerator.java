/*
 * Copyright 2010-2012 JetBrains s.r.o.
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

package org.jetbrains.jet.test.generator;

import com.google.common.collect.Lists;
import com.intellij.openapi.util.io.FileUtil;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

/**
 * @author abreslav
 */
public class TestGenerator {

    public interface TargetTestFramework {
        List<String> getImports();

        void generateSuiteClassAnnotations(@NotNull TestGenerator testGenerator, @NotNull Printer p);
        void generateExtraSuiteClassMethods(@NotNull TestGenerator testGenerator, @NotNull Printer p);
        void generateTestMethodAnnotations(@NotNull TestGenerator testGenerator, @NotNull Printer p);
        String getIsTestMethodCondition(@NotNull String methodVariableName);
    }

    public enum TargetTestFrameworks implements TargetTestFramework {
        JUNIT_3 {
            @Override
            public List<String> getImports() {
                return Arrays.asList(
                        "junit.framework.Assert",
                        "junit.framework.Test",
                        "junit.framework.TestSuite"
                );
            }

            @Override
            public void generateSuiteClassAnnotations(@NotNull TestGenerator testGenerator, @NotNull Printer p) {
                // Do nothing
            }

            @Override
            public void generateExtraSuiteClassMethods(@NotNull TestGenerator testGenerator, @NotNull Printer p) {
                p.println("public static Test suite() {");
                p.pushIndent();
                p.println("TestSuite suite = new TestSuite();");

                for (TestDataSource testDataSource : testGenerator.testDataSources) {
                    p.println("suite.addTestSuite(", testDataSource.getTestClassName(), ".class);");
                }

                p.println("return suite;");
                p.popIndent();
                p.println("}");
            }

            @Override
            public void generateTestMethodAnnotations(@NotNull TestGenerator testGenerator, @NotNull Printer p) {
                // Do nothing
            }

            @Override
            public String getIsTestMethodCondition(@NotNull String methodVariableName) {
                return "method.getName().startsWith(\"test\")";
            }
        },
        JUNIT_4 {
            @Override
            public List<String> getImports() {
                return Arrays.asList(
                        "org.junit.Assert",
                        "org.junit.Test",
                        "org.junit.runner.RunWith",
                        "org.junit.runners.Suite"
                );
            }

            @Override
            public void generateSuiteClassAnnotations(@NotNull TestGenerator testGenerator, @NotNull Printer p) {
                p.println("@RunWith(Suite.class)");
                p.println("@Suite.SuiteClasses({");
                p.pushIndent();
                for (Iterator<TestDataSource> iterator = testGenerator.testDataSources.iterator(); iterator.hasNext(); ) {
                    TestDataSource testDataSource = iterator.next();
                    p.print(testGenerator.suiteClassName, ".", testDataSource.getTestClassName(), ".class");
                    if (iterator.hasNext()) {
                        p.printWithNoIndent(",");
                    }
                    p.println();
                }
                p.popIndent();
                p.println("})");
            }

            @Override
            public void generateExtraSuiteClassMethods(@NotNull TestGenerator testGenerator, @NotNull Printer p) {
                // Do nothing
            }

            @Override
            public void generateTestMethodAnnotations(@NotNull TestGenerator testGenerator, @NotNull Printer p) {
                p.println("@Test");
            }

            @Override
            public String getIsTestMethodCondition(@NotNull String methodVariableName) {
                return "method.isAnnotationPresent(Test.class)";
            }
        }
    }

    public static FileFilter filterFilesByExtension(@NotNull final String extension) {
        return new FileFilter() {
            @Override
            public boolean accept(File file) {
                return file.isDirectory() || file.getName().endsWith("." + extension);
            }
        };
    }

    private final String baseDir;
    private final String testDataFileExtension;
    private final String suiteClassPackage;
    private final String suiteClassName;
    private final String baseTestClassPackage;
    private final String baseTestClassName;
    private final Collection<TestDataSource> testDataSources;
    private final String generatorName;
    private final TargetTestFramework targetTestFramework;

    public TestGenerator(
        @NotNull String baseDir,
        @NotNull String testDataFileExtension,
        @NotNull String suiteClassPackage,
        @NotNull String suiteClassName,
        @NotNull String baseTestClassPackage,
        @NotNull String baseTestClassName,
        @NotNull Collection<TestDataSource> testDataSources,
        @NotNull String generatorName
) {
        this(baseDir, testDataFileExtension, suiteClassPackage, suiteClassName, baseTestClassPackage, baseTestClassName, testDataSources,
             generatorName, TargetTestFrameworks.JUNIT_4);
    }

    public TestGenerator(
            @NotNull String baseDir,
            @NotNull String testDataFileExtension,
            @NotNull String suiteClassPackage,
            @NotNull String suiteClassName,
            @NotNull String baseTestClassPackage,
            @NotNull String baseTestClassName,
            @NotNull Collection<TestDataSource> testDataSources,
            @NotNull String generatorName,
            @NotNull TargetTestFramework targetTestFramework
    ) {
        this.baseDir = baseDir;
        this.testDataFileExtension = testDataFileExtension;
        this.suiteClassPackage = suiteClassPackage;
        this.suiteClassName = suiteClassName;
        this.baseTestClassPackage = baseTestClassPackage;
        this.baseTestClassName = baseTestClassName;
        this.testDataSources = testDataSources;
        this.generatorName = generatorName;
        this.targetTestFramework = targetTestFramework;
    }

    public void generateAndSave() throws IOException {
        StringBuilder out = new StringBuilder();
        Printer p = new Printer(out);

        p.print(FileUtil.loadFile(new File("injector-generator/copyright.txt")));
        p.println("package ", suiteClassPackage, ";");
        p.println();
        for (String importedClassName : targetTestFramework.getImports()) {
            p.println("import ", importedClassName, ";");
        }
        p.println();

        p.println("import java.io.File;");
        p.println("import java.io.FileFilter;");
        p.println("import java.lang.reflect.Method;");
        p.println("import java.util.HashSet;");
        p.println("import java.util.Set;");
        p.println();

        p.println("import ", baseTestClassPackage, ".", baseTestClassName, ";");
        p.println();

        p.println("/* This class is generated by ", generatorName, ". DO NOT MODIFY MANUALLY */");
        targetTestFramework.generateSuiteClassAnnotations(this, p);
        p.println("public class ", suiteClassName, " {");
        p.pushIndent();

        for (TestDataSource testDataSource : testDataSources) {
            generateTestClass(p, testDataSource);
            p.println();
        }

        targetTestFramework.generateExtraSuiteClassMethods(this, p);

        p.popIndent();
        p.println("}");

        String testSourceFilePath = baseDir + "/" + suiteClassPackage.replace(".", "/") + "/" + suiteClassName + ".java";
        File testSourceFile = new File(testSourceFilePath);
        FileUtil.writeToFile(testSourceFile, out.toString());
        System.out.println("Output written to file:\n" + testSourceFile.getAbsolutePath());
    }

    private void generateTestClass(Printer p, TestDataSource testDataSource) {
        p.println("public static class ", testDataSource.getTestClassName(), " extends ", baseTestClassName, " {");
        p.pushIndent();

        Collection<TestDataFile> files = Lists.newArrayList();
        files.addAll(testDataSource.getFiles());

        targetTestFramework.generateTestMethodAnnotations(this, p);
        p.println("public void " + testDataSource.getAllTestsPresentMethodName() + "() throws Exception {");
        p.pushIndent();

        testDataSource.getAllTestsPresentCheck(p);

        p.popIndent();
        p.println("}");
        p.println();

        for (TestDataFile file : files) {
            targetTestFramework.generateTestMethodAnnotations(this, p);
            p.println("public void ", file.getTestMethodName(), "() throws Exception {");
            p.pushIndent();

            p.println(file.getTestCall());

            p.popIndent();
            p.println("}");
            p.println();
        }

        generateAllTestsPresent(p);

        p.popIndent();
        p.println("}");
    }

    private void generateAllTestsPresent(Printer p) {
        String isTestMethod = targetTestFramework.getIsTestMethodCondition("method");
        String[] methodText = new String[] {
                "public static void allTestsPresent(Class<?> clazz, File testDataDir, boolean recursive) {",
                "    Set<String> methodNames = new HashSet<String>();",
                "    for (Method method : clazz.getDeclaredMethods()) {",
                "        if (" + isTestMethod + ") {",
                "            methodNames.add(method.getName().toLowerCase() + \"." + testDataFileExtension + "\");",
                "        }",
                "    }",
                "    for (File file : testDataDir.listFiles()) {",
                "        if (file.isDirectory()) {",
                "            if (recursive) {",
                "                allTestsPresent(clazz, file, recursive);",
                "            }",
                "        }",
                "        else {",
                "            String name = file.getName();",
                "            if (name.endsWith(\"." + testDataFileExtension + "\") && !methodNames.contains(\"test\" + name.toLowerCase())) {",
                "                Assert.fail(\"Test data file missing from the generated test class: \" + file + \"\\nPlease re-run the generator: " + generatorName + "\");",
                "            }",
                "        }",
                "    }",
                "}"};

        for (String s : methodText) {
            p.println(s);
        }
    }
}
