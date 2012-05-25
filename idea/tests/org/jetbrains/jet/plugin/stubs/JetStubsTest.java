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

package org.jetbrains.jet.plugin.stubs;

import com.intellij.psi.PsiFile;
import com.intellij.testFramework.LightProjectDescriptor;
import com.intellij.testFramework.fixtures.LightCodeInsightFixtureTestCase;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.psi.JetClass;
import org.jetbrains.jet.lang.psi.JetDeclaration;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.psi.stubs.PsiJetClassStub;
import org.jetbrains.jet.lang.psi.stubs.elements.JetStubElementTypes;
import org.jetbrains.jet.plugin.JetLightProjectDescriptor;

import java.util.List;

/**
 * @author yole
 */
public class JetStubsTest extends LightCodeInsightFixtureTestCase {
    @NotNull
    @Override
    protected LightProjectDescriptor getProjectDescriptor() {
        return JetLightProjectDescriptor.INSTANCE;
    }

    public void testSuperclassNames() {
        final PsiFile psiFile = myFixture.configureByText("foo.kt", "import java.util.ArrayList as alist\nclass C(): alist() { }");
        final List<JetDeclaration> declarations = ((JetFile) psiFile).getDeclarations();
        final JetClass jetClass = (JetClass) declarations.get(0);
        final PsiJetClassStub stub = JetStubElementTypes.CLASS.createStub(jetClass, null);
        final List<String> names = stub.getSuperNames();
        assertSameElements(names, "ArrayList", "alist");
    }
}
