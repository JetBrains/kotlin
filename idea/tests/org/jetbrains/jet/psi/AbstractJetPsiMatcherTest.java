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

package org.jetbrains.jet.psi;

import com.intellij.openapi.util.io.FileUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.InTextDirectivesUtils;
import org.jetbrains.jet.JetLiteFixture;
import org.jetbrains.jet.cli.jvm.compiler.JetCoreEnvironment;
import org.jetbrains.jet.config.CompilerConfiguration;
import org.jetbrains.jet.lang.psi.JetExpression;
import org.jetbrains.jet.lang.psi.JetPsiFactory;
import org.jetbrains.jet.lang.psi.JetTypeReference;
import org.jetbrains.jet.plugin.util.JetPsiMatcher;

import java.io.File;

import static org.jetbrains.jet.lang.psi.PsiPackage.JetPsiFactory;

public abstract class AbstractJetPsiMatcherTest extends JetLiteFixture {
    public void doTestExpressions(@NotNull String path) throws Exception {
        String fileText = FileUtil.loadFile(new File(path), true);
        String fileText2 = FileUtil.loadFile(new File(path + ".2"), true);

        boolean equalityExpected = InTextDirectivesUtils.findStringWithPrefixes(fileText, "// NOT_EQUAL") == null;

        JetPsiFactory psiFactory = JetPsiFactory(getProject());
        JetExpression expr = psiFactory.createExpression(fileText);
        JetExpression expr2 = psiFactory.createExpression(fileText2);

        assertTrue(
                "JetPsiMatcher.checkElementMatch() should return " + equalityExpected,
                equalityExpected == JetPsiMatcher.checkElementMatch(expr, expr2)
        );
    }

    public void doTestTypes(@NotNull String path) throws Exception {
        String fileText = FileUtil.loadFile(new File(path), true);
        String fileText2 = FileUtil.loadFile(new File(path + ".2"), true);

        boolean equalityExpected = InTextDirectivesUtils.findStringWithPrefixes(fileText, "// NOT_EQUAL") == null;

        JetPsiFactory psiFactory = JetPsiFactory(getProject());
        JetTypeReference typeRef = psiFactory.createProperty(fileText).getTypeRef();
        JetTypeReference typeRef2 = psiFactory.createProperty(fileText2).getTypeRef();

        assertNotNull(typeRef);
        assertNotNull(typeRef2);

        assertTrue(
                "JetPsiMatcher.checkElementMatch() should return " + equalityExpected,
                equalityExpected == JetPsiMatcher.checkElementMatch(typeRef, typeRef2)
        );
    }

    @Override
    protected JetCoreEnvironment createEnvironment() {
        return JetCoreEnvironment.createForTests(getTestRootDisposable(), new CompilerConfiguration());
    }
}
