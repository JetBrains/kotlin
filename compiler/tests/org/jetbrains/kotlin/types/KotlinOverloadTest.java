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

package org.jetbrains.kotlin.types;

import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment;
import org.jetbrains.kotlin.descriptors.FunctionDescriptor;
import org.jetbrains.kotlin.descriptors.ModuleDescriptor;
import org.jetbrains.kotlin.psi.KtNamedFunction;
import org.jetbrains.kotlin.psi.KtPsiFactoryKt;
import org.jetbrains.kotlin.resolve.FunctionDescriptorResolver;
import org.jetbrains.kotlin.resolve.OverloadUtil;
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowInfoFactory;
import org.jetbrains.kotlin.resolve.jvm.platform.JvmPlatform;
import org.jetbrains.kotlin.resolve.scopes.LexicalScope;
import org.jetbrains.kotlin.test.ConfigurationKind;
import org.jetbrains.kotlin.test.KotlinTestUtils;
import org.jetbrains.kotlin.test.KotlinTestWithEnvironment;
import org.jetbrains.kotlin.tests.di.InjectionKt;

public class KotlinOverloadTest extends KotlinTestWithEnvironment {
    private final ModuleDescriptor root = KotlinTestUtils.createEmptyModule("<test_root>");
    private FunctionDescriptorResolver functionDescriptorResolver;

    @Override
    protected KotlinCoreEnvironment createEnvironment() {
        return createEnvironmentWithMockJdk(ConfigurationKind.ALL);
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
        functionDescriptorResolver = InjectionKt.createContainerForTests(getProject(), root).getFunctionDescriptorResolver();
    }

    @Override
    protected void tearDown() throws Exception {
        functionDescriptorResolver = null;
        super.tearDown();
    }

    public void testBasic() throws Exception {
        assertNotOverloadable(
                "fun a() : Int",
                "fun a() : Int");

        assertNotOverloadable(
                "fun a() : Int",
                "fun a() : Any");

        assertNotOverloadable(
                "fun <T1> a() : T1",
                "fun <T> a() : T");

        assertNotOverloadable(
                "fun <T1> a(a : T1) : T1",
                "fun <T> a(a : T) : T");

        assertNotOverloadable(
                "fun <T1, X : T1> a(a : T1) : T1",
                "fun <T, Y : T> a(a : T) : T");

        assertNotOverloadable(
                "fun <T1, X : T1> a(a : T1) : T1",
                "fun <T, Y : T> a(a : T) : Y");

        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////


        assertNotOverloadable(
                "fun a() : Int",
                "fun a() : Any");

        assertOverloadable(
                "fun a(a : Int) : Int",
                "fun a() : Int");

        assertOverloadable(
                "fun a() : Int",
                "fun a(a : Int) : Int");

        assertOverloadable(
                "fun a(a : Int?) : Int",
                "fun a(a : Int) : Int");

        assertOverloadable(
                "fun <T> a(a : Int) : Int",
                "fun a(a : Int) : Int");

        assertNotOverloadable(
                "fun <T1, X : T1> a(a : T1) : T1",
                "fun <T, Y> a(a : T) : T");

        assertNotOverloadable(
                "fun <T1, X : T1> a(a : T1) : T1",
                "fun <T, Y : T> a(a : Y) : T");

        assertNotOverloadable(
                "fun <T1, X : T1> a(a : T1) : X",
                "fun <T, Y : T> a(a : T) : T");

        assertNotOverloadable(
                "fun <T1, X : Array<out T1>> a(a : Array<in T1>) : T1",
                "fun <T, Y : Array<out T>> a(a : Array<in T>) : T");

        assertNotOverloadable(
                "fun <T1, X : Array<T1>> a(a : Array<in T1>) : T1",
                "fun <T, Y : Array<out T>> a(a : Array<in T>) : T");

        assertNotOverloadable(
                "fun <T1, X : Array<out T1>> a(a : Array<in T1>) : T1",
                "fun <T, Y : Array<in T>> a(a : Array<in T>) : T");

        assertNotOverloadable(
                "fun <T1, X : Array<out T1>> a(a : Array<in T1>) : T1",
                "fun <T, Y : Array<*>> a(a : Array<in T>) : T");

        assertNotOverloadable(
                "fun <T1, X : Array<out T1>> a(a : Array<in T1>) : T1",
                "fun <T, Y : Array<out T>> a(a : Array<out T>) : T");

        assertNotOverloadable(
                "fun <T1, X : Array<out T1>> a(a : Array<*>) : T1",
                "fun <T, Y : Array<out T>> a(a : Array<in T>) : T");

        assertOverloadable(
                "fun ff() : Int",
                "fun Int.ff() : Int"
        );
    }

    private void assertNotOverloadable(String funA, String funB) {
        assertOverloadabilityRelation(funA, funB, true);
    }

    private void assertOverloadable(String funA, String funB) {
        assertOverloadabilityRelation(funA, funB, false);
    }

    private void assertOverloadabilityRelation(String funA, String funB, boolean expectedIsError) {
        FunctionDescriptor a = makeFunction(funA);
        FunctionDescriptor b = makeFunction(funB);

        boolean aOverloadableWithB = OverloadUtil.isOverloadable(a, b);
        assertEquals(expectedIsError, !aOverloadableWithB);

        boolean bOverloadableWithA = OverloadUtil.isOverloadable(b, a);
        assertEquals(expectedIsError, !bOverloadableWithA);
    }

    private FunctionDescriptor makeFunction(String funDecl) {
        KtNamedFunction function = KtPsiFactoryKt.KtPsiFactory(getProject()).createFunction(funDecl);
        LexicalScope scope = TypeTestUtilsKt.builtInPackageAsLexicalScope(JvmPlatform.INSTANCE.getBuiltIns());
        return functionDescriptorResolver.resolveFunctionDescriptor(root, scope, function, KotlinTestUtils.DUMMY_TRACE, DataFlowInfoFactory.EMPTY);
    }
}
