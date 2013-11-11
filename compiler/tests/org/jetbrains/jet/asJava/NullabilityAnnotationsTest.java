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

import com.intellij.psi.PsiClass;
import com.intellij.psi.impl.compiled.ClsElementImpl;
import com.intellij.psi.search.GlobalSearchScope;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.JetTestUtils;
import org.jetbrains.jet.cli.jvm.JVMConfigurationKeys;
import org.jetbrains.jet.config.CompilerConfiguration;

import java.io.File;
import java.util.Collections;
import java.util.List;

public class NullabilityAnnotationsTest extends KotlinAsJavaTestBase {
    private final File testDir = new File("compiler/testData/asJava/nullabilityAnnotations");

    @Override
    protected List<File> getKotlinSourceRoots() {
        return Collections.singletonList(new File(testDir, getTestName(false) + ".kt"));
    }

    @Override
    protected void extraConfiguration(@NotNull CompilerConfiguration configuration) {
        configuration.add(JVMConfigurationKeys.CLASSPATH_KEY, JetTestUtils.getAnnotationsJar());
    }

    public void test_DefaultPackage() throws Exception {
        doTest(getTestName(false));
    }

    public void testClass() throws Exception {
        doTest(getTestName(false));
    }

    public void testTrait() throws Exception {
        doTest(getTestName(false));
    }

    public void testClassWithConstructorAndProperties() throws Exception {
        doTest(getTestName(false));
    }

    public void testClassWithConstructor() throws Exception {
        doTest(getTestName(false));
    }

    public void testSynthetic() throws Exception {
        doTest(getTestName(false));
    }

    public void testPrimitives() throws Exception {
        doTest(getTestName(false));
    }

    public void testPrivateInClass() throws Exception {
        doTest(getTestName(false));
    }

    public void testPrivateInTrait() throws Exception {
        doTest(getTestName(false));
    }

    private void doTest(@NotNull String fqName) {
        PsiClass psiClass = finder.findClass(fqName, GlobalSearchScope.allScope(getProject()));
        if (!(psiClass instanceof KotlinLightClass)) {
            throw new IllegalStateException("Not a light class: " + psiClass + " (" + fqName + ")");
        }

        PsiClass delegate = ((KotlinLightClass) psiClass).getDelegate();
        if (!(delegate instanceof ClsElementImpl)) {
            throw new IllegalStateException("Not a CLS element: " + delegate);
        }

        StringBuilder buffer = new StringBuilder();
        ((ClsElementImpl) delegate).appendMirrorText(0, buffer);
        String actual = buffer.toString();

        JetTestUtils.assertEqualsToFile(new File(testDir, fqName + ".java"), actual);
    }
}
