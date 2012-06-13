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

import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import com.google.common.collect.Lists;
import com.intellij.openapi.util.text.StringUtil;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileFilter;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
* @author abreslav
*/
public class TestDataSource {
    private final File rootFile;
    private final boolean recursive;
    private final FileFilter filter;
    private final String doTestMethodName;
    private final String testClassName;

    public TestDataSource(@NotNull File rootFile, boolean recursive, @NotNull FileFilter filter, String doTestMethodName) {
        this.rootFile = rootFile;
        this.recursive = recursive;
        this.filter = filter;
        this.doTestMethodName = doTestMethodName;
        this.testClassName = StringUtil.capitalize(rootFile.getName());
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
            if (file.isDirectory()) {
                if (recursive) {
                    collectFiles(file, result, recursive);
                }
            }
            else {
                result.add(file);
            }
        }
    }

    public void getAllTestsPresentCheck(@NotNull Printer p) {
        p.println("allTestsPresent(" + getTestClassName() + ".class, new File(\"" + rootFile + "\"), " + recursive + ");");
    }

    public String getAllTestsPresentMethodName() {
        return "allTestsPresentIn" + StringUtil.capitalize(rootFile.getName());
    }

    public String getTestClassName() {
        return testClassName;
    }
}
