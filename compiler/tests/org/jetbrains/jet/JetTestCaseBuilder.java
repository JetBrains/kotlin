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

package org.jetbrains.jet;

import com.intellij.openapi.application.PathManager;
import junit.framework.Test;
import junit.framework.TestSuite;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.parsing.JetParser;
import org.jetbrains.jet.lang.parsing.JetParserDefinition;

import java.io.File;
import java.io.FileFilter;
import java.io.FilenameFilter;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public abstract class JetTestCaseBuilder {
    public static FilenameFilter emptyFilter = new FilenameFilter() {
        @Override
        public boolean accept(File file, String name) {
            return true;
        }
    };

    public static String getTestDataPathBase() {
        return getHomeDirectory() + "/compiler/testData";
    }

    public static String getHomeDirectory() {
       return new File(PathManager.getResourceRoot(JetTestCaseBuilder.class, "/org/jetbrains/jet/JetTestCaseBuilder.class")).getParentFile().getParentFile().getParent();
    }

    public interface NamedTestFactory {
        @NotNull Test createTest(@NotNull String dataPath, @NotNull String name, @NotNull File file);
    }

    @NotNull
    public static TestSuite suiteForDirectory(String baseDataDir, @NotNull final String dataPath, boolean recursive, @NotNull NamedTestFactory factory) {
        return suiteForDirectory(baseDataDir, dataPath, recursive, kotlinFilter, factory);
    }    
    
    @NotNull
    public static TestSuite suiteForDirectory(String baseDataDir, @NotNull final String dataPath, boolean recursive,
            @NotNull final FilenameFilter filter, @NotNull NamedTestFactory factory) {
        TestSuite suite = new TestSuite(dataPath);
        appendTestsInDirectory(baseDataDir, dataPath, recursive, filter, factory, suite);
        return suite;
    }

    public static FilenameFilter kotlinFilter = new FilenameFilter() {
        @Override
        public boolean accept(File dir, String name) {
            return name.endsWith(".kt") || name.endsWith(".jet") || name.endsWith("." + JetParserDefinition.KTSCRIPT_FILE_SUFFIX);
        }
    };

    @NotNull
    public static FilenameFilter and(@NotNull final FilenameFilter a, @NotNull final FilenameFilter b) {
        return new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return a.accept(dir, name) && b.accept(dir, name);
            }
        };
    }

    public static void appendTestsInDirectory(String baseDataDir, String dataPath, boolean recursive, @NotNull FilenameFilter filter, NamedTestFactory factory, TestSuite suite) {
        final String extensionJet = ".jet";
        final String extensionKt = ".kt";
        File dir = new File(baseDataDir + dataPath);
        FileFilter dirFilter = new FileFilter() {
            @Override
            public boolean accept(File pathname) {
                return pathname.isDirectory();
            }
        };
        if (recursive) {
            File[] files = dir.listFiles(dirFilter);
            assert files != null : dir;
            List<File> subdirs = Arrays.asList(files);
            Collections.sort(subdirs);
            for (File subdir : subdirs) {
                Test suiteForDir = suiteForDirectory(baseDataDir, dataPath + "/" + subdir.getName(), recursive, filter, factory);
                if (suiteForDir.countTestCases() != 0) {
                    suite.addTest(suiteForDir);
                }
            }
        }
        List<File> files = Arrays.asList(dir.listFiles(filter));
        Collections.sort(files);
        for (File file : files) {
            String fileName = file.getName();
            String testName =
                    fileName.endsWith(extensionJet) ? fileName.substring(0, fileName.length() - extensionJet.length()) :
                    fileName.endsWith(extensionKt) ? fileName.substring(0, fileName.length() - extensionKt.length()) :
                    fileName;
            suite.addTest(factory.createTest(dataPath, testName, file));
        }
    }
}
