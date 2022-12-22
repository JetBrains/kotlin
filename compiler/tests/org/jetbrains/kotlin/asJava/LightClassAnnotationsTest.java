/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiModifierList;
import com.intellij.psi.search.GlobalSearchScope;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.asJava.classes.KtLightClass;
import org.jetbrains.kotlin.cli.jvm.config.JvmContentRootsKt;
import org.jetbrains.kotlin.config.CompilerConfiguration;
import org.jetbrains.kotlin.test.KotlinTestUtils;
import org.jetbrains.kotlin.test.util.KtTestUtil;

import java.io.File;
import java.util.Collections;
import java.util.List;

public class LightClassAnnotationsTest extends KotlinAsJavaTestBase {
    private final File testDir = new File("compiler/testData/asJava/lightClasses/annotations");

    @Override
    protected List<File> getKotlinSourceRoots() {
        return Collections.singletonList(new File(testDir, getTestName(false) + ".kt"));
    }

    @Override
    protected void extraConfiguration(@NotNull CompilerConfiguration configuration) {
        JvmContentRootsKt.addJvmClasspathRoot(configuration, KtTestUtil.getAnnotationsJar());
    }

    public void testExtraAnnotations() throws Exception {
        doTest(getTestName(false));
    }

    public void testNestedClass() throws Exception {
        doTest("Outer.Nested");
    }

    private void doTest(@NotNull String fqName) {
        PsiClass psiClass = finder.findClass(fqName, GlobalSearchScope.allScope(getProject()));
        if (!(psiClass instanceof KtLightClass)) {
            throw new IllegalStateException("Not a light class: " + psiClass + " (" + fqName + ")");
        }

        PsiModifierList modifierList = psiClass.getModifierList();
        assert modifierList != null : "No modifier list for " + psiClass.getText();

        StringBuilder sb = new StringBuilder();
        for (PsiAnnotation annotation : modifierList.getAnnotations()) {
            sb.append(annotation.getText()).append("\n");
        }

        KotlinTestUtils.assertEqualsToFile(new File(testDir, getTestName(false) + ".annotations.txt"), sb.toString());
    }
}
