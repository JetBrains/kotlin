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

package org.jetbrains.jet.types;

import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.jet.ConfigurationKind;
import org.jetbrains.jet.cli.jvm.compiler.JetCoreEnvironment;
import org.jetbrains.jet.lang.descriptors.FunctionDescriptor;
import org.jetbrains.jet.lang.descriptors.ModuleDescriptor;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.psi.JetPsiFactory;
import org.jetbrains.jet.lang.resolve.DescriptorUtils;
import org.jetbrains.jet.lang.resolve.lazy.KotlinTestWithEnvironment;
import org.jetbrains.jet.lang.resolve.lazy.LazyResolveTestUtil;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.renderer.DescriptorRenderer;

import java.util.Collection;
import java.util.Collections;

public class BoundsSubstitutorTest extends KotlinTestWithEnvironment {
    @Override
    protected JetCoreEnvironment createEnvironment() {
        return createEnvironmentWithMockJdk(ConfigurationKind.JDK_ONLY);
    }

    public void testSimpleSubstitution() throws Exception {
        doTest("fun <T> f(l: List<T>): T",
               "fun <T> f(l: jet.List<jet.Any?>): jet.Any?");
    }

    public void testParameterInBound() throws Exception {
        doTest("fun <T, R : List<T>> f(l: List<R>): R",
               "fun <T, R : jet.List<jet.Any?>> f(l: jet.List<jet.List<jet.Any?>>): jet.List<jet.Any?>");
    }

    public void testWithWhere() throws Exception {
        doTest("fun <T> f(l: List<T>): T where T : Any",
               "fun <T : jet.Any> f(l: jet.List<jet.Any>): jet.Any");
    }

    public void testWithWhereTwoBounds() throws Exception {
        doTest("fun <T, R> f(l: List<R>): R where T : List<R>, R : Any",
               "fun <T : jet.List<jet.Any>, R : jet.Any> f(l: jet.List<jet.Any>): jet.Any");
    }

    public void testWithWhereTwoBoundsReversed() throws Exception {
        doTest("fun <T, R> f(l: List<R>): R where T : Any, R : List<T>",
               "fun <T : jet.Any, R : jet.List<jet.Any>> f(l: jet.List<jet.List<jet.Any>>): jet.List<jet.Any>");
    }

    //public void testWithWhereTwoBoundsLoop() throws Exception {
    //    doTest("fun <T, R> f(l: List<R>): R where T : R, R : T",
    //           "");
    //}

    private void doTest(String text, String expected) {
        JetFile jetFile = JetPsiFactory.createFile(getProject(), "fun.kt", text);
        ModuleDescriptor module = LazyResolveTestUtil.resolveLazily(Collections.singletonList(jetFile), getEnvironment());
        Collection<FunctionDescriptor> functions = module.getPackage(FqName.ROOT).getMemberScope().getFunctions(Name.identifier("f"));
        assert functions.size() == 1 : "Many functions defined";
        FunctionDescriptor function = ContainerUtil.getFirstItem(functions);

        FunctionDescriptor substituted = DescriptorUtils.substituteBounds(function);
        String actual = DescriptorRenderer.COMPACT.render(substituted);
        assertEquals(expected, actual);
    }
}
