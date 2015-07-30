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

package org.jetbrains.kotlin.asJava;

import com.google.common.collect.Sets;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiFile;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.testFramework.LightProjectDescriptor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.idea.test.JetLightCodeInsightFixtureTestCase;
import org.jetbrains.kotlin.idea.test.JetLightProjectDescriptor;
import org.jetbrains.kotlin.name.FqName;
import org.jetbrains.kotlin.psi.JetFile;

import java.util.Set;

public class FakeLightClassForPackageTest extends JetLightCodeInsightFixtureTestCase {
    private static final String TEST_DATA_PATH = "idea/testData/fakeLightClassForPackage/";

    public void testMultiFile() {
        myFixture.configureByFiles(TEST_DATA_PATH + "1.kt", TEST_DATA_PATH + "2.kt");
        GlobalSearchScope searchScope = GlobalSearchScope.allScope(getProject());
        PsiClass[] classes = JavaElementFinder.getInstance(getProject()).findClasses("test.TestPackage", searchScope);

        assertEquals(3, classes.length);

        assertInstanceOf(classes[0], KotlinLightClassForFacade.class);

        Set<JetFile> expectedFiles = Sets.newHashSet(
                LightClassGenerationSupport.getInstance(getProject()).findFilesForPackage(new FqName("test"), searchScope)
        );

        Set<PsiFile> actualFiles = Sets.newHashSet();
        for (int i = 1; i < classes.length; i++) {
            assertInstanceOf(classes[i], FakeLightClassForFileOfPackage.class);
            actualFiles.add(((FakeLightClassForFileOfPackage) classes[i]).getContainingFile());
        }

        assertEquals(expectedFiles, actualFiles);
    }

    @NotNull
    @Override
    protected LightProjectDescriptor getProjectDescriptor() {
        return JetLightProjectDescriptor.INSTANCE;
    }
}
