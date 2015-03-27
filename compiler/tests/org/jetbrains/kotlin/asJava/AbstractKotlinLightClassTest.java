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

import com.intellij.openapi.util.io.FileUtil;
import com.intellij.psi.PsiClass;
import com.intellij.psi.impl.compiled.ClsElementImpl;
import com.intellij.psi.search.GlobalSearchScope;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.checkers.KotlinMultiFileTestWithWithJava;
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment;
import org.jetbrains.kotlin.test.JetTestUtils;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class AbstractKotlinLightClassTest extends KotlinMultiFileTestWithWithJava<Void, Void> {
    private static final Pattern SUBJECT_FQ_NAME_PATTERN = Pattern.compile("^//\\s*(.*)$", Pattern.MULTILINE);

    @NotNull
    @Override
    protected File getKotlinSourceRoot() {
        return createTmpDir("kotlin-src");
    }

    @NotNull
    public static JavaElementFinder createFinder(@NotNull KotlinCoreEnvironment environment) throws IOException {
        // We need to resolve all the files in order too fill in the trace that sits inside LightClassGenerationSupport
        JetTestUtils.resolveAllKotlinFiles(environment);

        return JavaElementFinder.getInstance(environment.getProject());
    }

    @Override
    protected void doMultiFileTest(File file, Map<String, ModuleAndDependencies> modules, List<Void> files) throws IOException {
        String text = FileUtil.loadFile(file, true);
        Matcher matcher = SUBJECT_FQ_NAME_PATTERN.matcher(text);
        assertTrue("No FqName specified. First line of the form '// f.q.Name' expected", matcher.find());
        String fqName = matcher.group(1);

        JavaElementFinder finder = createFinder(getEnvironment());

        PsiClass psiClass = finder.findClass(fqName, GlobalSearchScope.allScope(getEnvironment().getProject()));
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

        JetTestUtils.assertEqualsToFile(JetTestUtils.replaceExtension(file, "java"), actual);
    }

    @Override
    protected Void createTestModule(@NotNull String name) {
        return null;
    }

    @Override
    protected Void createTestFile(Void module, String fileName, String text, Map<String, String> directives) {
        return null;
    }
}
