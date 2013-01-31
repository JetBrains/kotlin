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

package org.jetbrains.jet.asJava;

import com.google.common.collect.Sets;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiFile;
import com.intellij.psi.search.GlobalSearchScope;
import org.jetbrains.jet.cli.jvm.compiler.CliLightClassGenerationSupport;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.resolve.name.FqName;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

public class JavaElementFinderMultiFileTest extends KotlinAsJavaTestBase {
    private static final String PREFIX = "compiler/testData/asJava/findClasses/multiFile/";

    @Override
    protected List<File> getKotlinSourceRoots() {
        return Arrays.asList(
                new File(PREFIX + getTestName(false) + "1.kt"),
                new File(PREFIX + getTestName(false) + "2.kt")
        );
    }

    public void testMultiFile() {
        GlobalSearchScope searchScope = GlobalSearchScope.allScope(getProject());
        PsiClass[] classes = finder.findClasses("test.TestPackage", searchScope);

        assertEquals(3, classes.length);

        assertInstanceOf(classes[0], KotlinLightClassForPackage.class);

        Set<JetFile> expectedFiles = Sets.newHashSet(
                CliLightClassGenerationSupport.getInstanceForCli(getProject()).findFilesForPackage(new FqName("test"), searchScope)
        );

        Set<PsiFile> actualFiles = Sets.newHashSet();
        for (int i = 1; i < classes.length; i++) {
            assertInstanceOf(classes[i], FakeLightClassForFileOfPackage.class);
            actualFiles.add(((FakeLightClassForFileOfPackage) classes[i]).getContainingFile());
        }

        assertEquals(expectedFiles, actualFiles);
    }
}
