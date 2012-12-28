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

package org.jetbrains.jet.plugin.stubs.resolve;

import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import com.google.common.collect.Lists;
import com.intellij.testFramework.fixtures.LightCodeInsightFixtureTestCase;
import org.jetbrains.jet.lang.psi.JetNamedFunction;
import org.jetbrains.jet.lang.psi.JetProperty;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.plugin.JetFileType;
import org.jetbrains.jet.plugin.stubindex.resolve.StubPackageMemberDeclarationProvider;

import java.util.Collection;
import java.util.List;

public class JetStubResolveTest extends LightCodeInsightFixtureTestCase {
    public void testSinglePackageFunction() {
        myFixture.configureByText(JetFileType.INSTANCE,
                                  "package test.testing\n" +
                                  "fun some() {}");
        StubPackageMemberDeclarationProvider provider =
                new StubPackageMemberDeclarationProvider(new FqName("test.testing"), getProject());

        List<JetNamedFunction> some = Lists.newArrayList(provider.getFunctionDeclarations(Name.identifier("some")));

        assertSize(1, some);
        assertEquals("fun some() {}", some.get(0).getText());
    }

    public void testMultiPackageFunction() {
        myFixture.configureByText(JetFileType.INSTANCE,
                                  "package test.testing\n" +
                                  "fun other(v : Int) = 12\n" +
                                  "fun other(v : String) {}");

        StubPackageMemberDeclarationProvider provider =
                new StubPackageMemberDeclarationProvider(new FqName("test.testing"), getProject());

        List<JetNamedFunction> other = Lists.newArrayList(provider.getFunctionDeclarations(Name.identifier("other")));
        Collection<String> functionTexts = Collections2.transform(other, new Function<JetNamedFunction, String>() {
            @Override
            public String apply(JetNamedFunction function) {
                return function.getText();
            }
        });

        assertSize(2, other);
        assertTrue(functionTexts.contains("fun other(v : Int) = 12"));
        assertTrue(functionTexts.contains("fun other(v : String) {}"));
    }

    public void testPackageProperty() {
        myFixture.configureByText(JetFileType.INSTANCE,
                                  "package test.testing\n" +
                                  "val test = 12\n");

        StubPackageMemberDeclarationProvider provider =
                new StubPackageMemberDeclarationProvider(new FqName("test.testing"), getProject());

        List<JetProperty> testProperties = Lists.newArrayList(provider.getPropertyDeclarations(Name.identifier("test")));

        assertSize(1, testProperties);
        assertEquals("val test = 12", testProperties.get(0).getText());
    }
}
