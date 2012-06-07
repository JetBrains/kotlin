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

package org.jetbrains.jet.lang.resolve.lazy;

import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import com.google.common.collect.Lists;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * @author abreslav
 */
public class LazyResolveComparingTestGenerator {
    public static void main(String[] args) throws IOException {
        new LazyResolveComparingTestGenerator().generateAndSave();
    }

    private static class TestDataSource {
        private final File rootFile;
        private final boolean recursive;
        private final FileFilter filter;
        private final String doTestMethodName;

        public TestDataSource(@NotNull File rootFile, boolean recursive, @NotNull FileFilter filter, String doTestMethodName) {
            this.rootFile = rootFile;
            this.recursive = recursive;
            this.filter = filter;
            this.doTestMethodName = doTestMethodName;
        }

        public Collection<TestDataFile> getFiles() {
            if (!rootFile.isDirectory()) {
                return Collections.singletonList(new TestDataFile(rootFile, doTestMethodName));
            }
            List<File> files = Lists.newArrayList();
            collectFiles(rootFile, files, recursive);
            return Collections2.transform(files, new Function<File, TestDataFile>() {
                @Override
                public TestDataFile apply(File file) {
                    return new TestDataFile(file, doTestMethodName);
                }
            });
        }

        private void collectFiles(File current, List<File> result, boolean recursive) {
            for (File file : current.listFiles(filter)) {
                if (file.isDirectory() && recursive) {
                    collectFiles(file, result, recursive);
                }
                else {
                    result.add(file);
                }
            }
        }
    }

    private static class TestDataFile {
        private final File file;
        private final String doTestMethodName;

        public TestDataFile(File file, String doTestMethodName) {
            this.file = file;
            this.doTestMethodName = doTestMethodName;
        }

        public String getTestCall() {
            return doTestMethodName + "(\"" + file + "\");";
        }

        public String getTestMethodName() {
            return "test" + FileUtil.getNameWithoutExtension(StringUtil.capitalize(file.getName()));
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
    private final String testClassPackage;
    private final String testClassName;
    private final String baseTestClassPackage;
    private final String baseTestClassName;
    private final Collection<TestDataSource> testDataSources;
    private final String generatorName;

    public LazyResolveComparingTestGenerator() {
        baseDir = "compiler/tests/";
        testDataFileExtension = "kt";
        testClassPackage = "org.jetbrains.jet.lang.resolve.lazy";
        testClassName = "LazyResolveComparingTestGenerated";
        baseTestClassPackage = "org.jetbrains.jet.lang.resolve.lazy";
        baseTestClassName = "AbstractLazyResolveComparingTest";
        //testDataDir = new File("compiler/testData/lazyResolve");
        testDataSources = Arrays.asList(
            new TestDataSource(new File("compiler/testData/readKotlinBinaryClass"), true, filterFilesByExtension(testDataFileExtension), "doTestSinglePackage"),
            new TestDataSource(new File("compiler/testData/lazyResolve"), true, filterFilesByExtension(testDataFileExtension), "doTest")
        );
        generatorName = "LazyResolveComparingTestGenerator";
    }

    public void generateAndSave() throws IOException {
        StringBuilder out = new StringBuilder();
        Printer p = new Printer(out);

        p.print(FileUtil.loadFile(new File("injector-generator/copyright.txt")));
        p.println("package ", testClassPackage, ";");
        p.println();
        p.println("import org.junit.Assert;");
        p.println("import org.junit.Test;");
        p.println();

        p.println("import java.io.File;");
        p.println("import java.io.FileFilter;");
        p.println("import java.lang.reflect.Method;");
        p.println("import java.util.HashSet;");
        p.println("import java.util.Set;");
        p.println();

        p.println("import ", baseTestClassPackage, ".", baseTestClassName, ";");
        p.println();

        p.println("/* This class is generated by " + generatorName + ". DO NOT MODIFY MANUALLY */");
        p.println("public class ", testClassName, " extends ", baseTestClassName, " {");
        p.pushIndent();

        Collection<TestDataFile> files = Lists.newArrayList();
        for (TestDataSource testDataSource : testDataSources) {
            files.addAll(testDataSource.getFiles());
        }

        for (TestDataFile file : files) {
            p.println("@Test");
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

        String testSourceFilePath = baseDir + testClassPackage.replace(".", "/") + "/" + testClassName + ".java";
        FileUtil.writeToFile(new File(testSourceFilePath), out.toString());
    }

    private void generateAllTestsPresent(Printer p) {
        String methodText =
                     //"@Test\n" +
                     "    public void allTestsPresent(String testDataDir) {\n" +
                     "        Set<String> methodNames = new HashSet<String>();\n" +
                     "        for (Method method : " + testClassName + ".class.getDeclaredMethods()) {\n" +
                     "            if (method.isAnnotationPresent(Test.class)) {\n" +
                     "                methodNames.add(method.getName().toLowerCase() + \"." + testDataFileExtension + "\");\n" +
                     "            }\n" +
                     "        }\n" +
                     "        File[] testDataFiles = new File(\"testDataDi\").listFiles(new FileFilter() {\n" +
                     "            @Override\n" +
                     "            public boolean accept(File pathname) {\n" +
                     "                return pathname.getName().endsWith(\"." + testDataFileExtension + "\");\n" +
                     "            }\n" +
                     "        });\n" +
                     "        for (File testDataFile : testDataFiles) {\n" +
                     "            if (!methodNames.contains(\"test\" + testDataFile.getName().toLowerCase())) {\n" +
                     "                Assert.fail(\"Test data file missing from the generated test class: \" + testDataFile + \"\\nPlease re-run the generator: " + testClassName + "\");\n" +
                     "            }\n" +
                     "        }\n" +
                     "    }\n";

        p.println(methodText);
    }

    private static class Printer {
        private static final String INDENTATION_UNIT = "    ";
        private String indent = "";
        private final StringBuilder out;

        private Printer(@NotNull StringBuilder out) {
            this.out = out;
        }

        public void println(Object... objects) {
            print(objects);
            out.append("\n");
        }

        public void print(Object... objects) {
            out.append(indent);
            for (Object object : objects) {
                out.append(object);
            }
        }

        public void pushIndent() {
            indent += INDENTATION_UNIT;
        }

        public void popIndent() {
            if (indent.length() < INDENTATION_UNIT.length()) {
                throw new IllegalStateException("No indentation to pop");
            }

            indent = indent.substring(INDENTATION_UNIT.length());
        }
    }
}
