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

import com.intellij.openapi.util.io.FileUtil;
import org.jetbrains.annotations.NotNull;

import java.io.File;
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

                for (TestClassModel testDataSource : testGenerator.testClassModels) {
                    p.println("suite.addTestSuite(", testDataSource.getName(), ".class);");
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
                for (Iterator<? extends TestClassModel> iterator = testGenerator.testClassModels.iterator(); iterator.hasNext(); ) {
                    TestClassModel testClassModel = iterator.next();
                    p.print(testGenerator.suiteClassName, ".", testClassModel.getName(), ".class");
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

    private final String baseDir;
    private final String testDataFileExtension;
    private final String suiteClassPackage;
    private final String suiteClassName;
    private final String baseTestClassPackage;
    private final String baseTestClassName;
    private final Collection<? extends TestClassModel> testClassModels;
    private final String generatorName;
    private final TargetTestFramework targetTestFramework;

    public TestGenerator(
        @NotNull String baseDir,
        @NotNull String testDataFileExtension,
        @NotNull String suiteClassPackage,
        @NotNull String suiteClassName,
        @NotNull String baseTestClassPackage,
        @NotNull String baseTestClassName,
        @NotNull Collection<? extends TestClassModel> testClassModels,
        @NotNull String generatorName
    ) {
        this(baseDir, testDataFileExtension, suiteClassPackage, suiteClassName, baseTestClassPackage, baseTestClassName, testClassModels,
             generatorName, TargetTestFrameworks.JUNIT_4);
    }

    public TestGenerator(
            @NotNull String baseDir,
            @NotNull String testDataFileExtension,
            @NotNull String suiteClassPackage,
            @NotNull String suiteClassName,
            @NotNull String baseTestClassPackage,
            @NotNull String baseTestClassName,
            @NotNull Collection<? extends TestClassModel> testClassModels,
            @NotNull String generatorName,
            @NotNull TargetTestFramework targetTestFramework
    ) {
        this.baseDir = baseDir;
        this.testDataFileExtension = testDataFileExtension;
        this.suiteClassPackage = suiteClassPackage;
        this.suiteClassName = suiteClassName;
        this.baseTestClassPackage = baseTestClassPackage;
        this.baseTestClassName = baseTestClassName;
        this.testClassModels = testClassModels;
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
        p.println("import org.jetbrains.jet.JetTestUtils;");
        p.println("import org.jetbrains.jet.test.TestMetadata;");
        p.println();

        p.println("import ", baseTestClassPackage, ".", baseTestClassName, ";");
        p.println();

        p.println("/* This class is generated by ", generatorName, ". DO NOT MODIFY MANUALLY */");
        targetTestFramework.generateSuiteClassAnnotations(this, p);
        if (testClassModels.size() == 1) {
            TestClassModel theOnlyTestClass = testClassModels.iterator().next();
            generateTestClass(p, new DelegatingTestClassModel(theOnlyTestClass) {
                @Override
                public String getName() {
                    return suiteClassName;
                }
            }, false);
        }
        else {
            p.println("public class ", suiteClassName, " {");
            p.pushIndent();

            for (TestClassModel testDataSource : testClassModels) {
                generateTestClass(p, testDataSource, true);
                p.println();
            }

            targetTestFramework.generateExtraSuiteClassMethods(this, p);

            p.popIndent();
            p.println("}");
        }

        String testSourceFilePath = baseDir + "/" + suiteClassPackage.replace(".", "/") + "/" + suiteClassName + ".java";
        File testSourceFile = new File(testSourceFilePath);
        FileUtil.writeToFile(testSourceFile, out.toString());
        System.out.println("Output written to file:\n" + testSourceFile.getAbsolutePath());
    }

    private void generateTestClass(Printer p, TestClassModel testDataSource, boolean isStatic) {
        String staticModifier = isStatic ? "static " : "";
        generateMetadata(p, testDataSource);
        p.println("public " + staticModifier + "class ", testDataSource.getName(), " extends ", baseTestClassName, " {");
        p.pushIndent();

        Collection<TestMethodModel> testMethods = testDataSource.getTestMethods();

        for (TestMethodModel testMethodModel : testMethods) {
            generateTestMethod(p, testMethodModel);
            p.println();
        }

        Collection<TestClassModel> innerTestClasses = testDataSource.getInnerTestClasses();
        for (TestClassModel innerTestClass : innerTestClasses) {
            generateTestClass(p, innerTestClass, true);
        }

        p.popIndent();
        p.println("}");
    }

    private void generateTestMethod(Printer p, TestMethodModel testMethodModel) {
        targetTestFramework.generateTestMethodAnnotations(this, p);
        generateMetadata(p, testMethodModel);
        p.println("public void ", testMethodModel.getName(), "() throws Exception {");
        p.pushIndent();

        testMethodModel.generateBody(p, generatorName);

        p.popIndent();
        p.println("}");
    }

    private void generateMetadata(Printer p, TestEntityModel testDataSource) {
        String dataString = testDataSource.getDataString();
        if (dataString != null) {
            p.println("@TestMetadata(\"", dataString, "\")");
        }
    }
}
