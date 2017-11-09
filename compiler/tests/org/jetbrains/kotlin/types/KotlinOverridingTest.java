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
import org.jetbrains.kotlin.container.ComponentProvider;
import org.jetbrains.kotlin.container.DslKt;
import org.jetbrains.kotlin.descriptors.FunctionDescriptor;
import org.jetbrains.kotlin.descriptors.ModuleDescriptor;
import org.jetbrains.kotlin.psi.KtNamedFunction;
import org.jetbrains.kotlin.psi.KtPsiFactoryKt;
import org.jetbrains.kotlin.resolve.FunctionDescriptorResolver;
import org.jetbrains.kotlin.resolve.OverridingUtil;
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowInfoFactory;
import org.jetbrains.kotlin.resolve.lazy.JvmResolveUtil;
import org.jetbrains.kotlin.resolve.scopes.LexicalScope;
import org.jetbrains.kotlin.test.ConfigurationKind;
import org.jetbrains.kotlin.test.KotlinTestUtils;
import org.jetbrains.kotlin.test.KotlinTestWithEnvironment;

public class KotlinOverridingTest extends KotlinTestWithEnvironment {
    private ModuleDescriptor module;
    private FunctionDescriptorResolver functionDescriptorResolver;

    @Override
    protected KotlinCoreEnvironment createEnvironment() {
        return createEnvironmentWithMockJdk(ConfigurationKind.ALL);
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
        ComponentProvider container = JvmResolveUtil.createContainer(getEnvironment());
        module = DslKt.getService(container, ModuleDescriptor.class);
        functionDescriptorResolver = DslKt.getService(container, FunctionDescriptorResolver.class);
    }

    @Override
    protected void tearDown() throws Exception {
        module = null;
        functionDescriptorResolver = null;
        super.tearDown();
    }

    public void testBasic() throws Exception {
        assertOverridable(
                "fun a() : Int",
                "fun a() : Int");

        assertOverridable(
                "fun <T1> a() : T1",
                "fun <T> a() : T");

        assertOverridable(
                "fun <T1> a(a : T1) : T1",
                "fun <T> a(a : T) : T");

        assertOverridable(
                "fun <T1, X : T1> a(a : T1) : T1",
                "fun <T, Y : T> a(a : T) : T");

        assertOverridable(
                "fun <T1, X : T1> a(a : T1) : T1",
                "fun <T, Y : T> a(a : T) : Y");

        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////


        assertNotOverridable(
                "fun ab() : Int",
                "fun a() : Int");

        assertOverridable(
                "fun a() : Int",
                "fun a() : Any");

        // return types are not cheked in the utility
        /*
        assertNotOverridable(
                "fun a() : Any",
                "fun a() : Int");
        */

        assertNotOverridable(
                "fun a(a : Int) : Int",
                "fun a() : Int");

        assertNotOverridable(
                "fun a() : Int",
                "fun a(a : Int) : Int");

        assertNotOverridable(
                "fun a(a : Int?) : Int",
                "fun a(a : Int) : Int");

        assertNotOverridable(
                "fun <T> a(a : Int) : Int",
                "fun a(a : Int) : Int");

        assertNotOverridable(
                "fun <T1, X : T1> a(a : T1) : T1",
                "fun <T, Y> a(a : T) : T");

        assertNotOverridable(
                "fun <T1, X : T1> a(a : T1) : T1",
                "fun <T, Y : T> a(a : Y) : T");

        assertOverridable(
                "fun <T1, X : T1> a(a : T1) : X",
                "fun <T, Y : T> a(a : T) : T");

        assertOverridable(
                "fun <T1, X : Array<out T1>> a(a : Array<in T1>) : T1",
                "fun <T, Y : Array<out T>> a(a : Array<in T>) : T");

        assertNotOverridable(
                "fun <T1, X : Array<T1>> a(a : Array<in T1>) : T1",
                "fun <T, Y : Array<out T>> a(a : Array<in T>) : T");

        assertNotOverridable(
                "fun <T1, X : Array<out T1>> a(a : Array<in T1>) : T1",
                "fun <T, Y : Array<in T>> a(a : Array<in T>) : T");

        assertNotOverridable(
                "fun <T1, X : Array<out T1>> a(a : Array<in T1>) : T1",
                "fun <T, Y : Array<*>> a(a : Array<in T>) : T");

        assertNotOverridable(
                "fun <T1, X : Array<out T1>> a(a : Array<in T1>) : T1",
                "fun <T, Y : Array<out T>> a(a : Array<out T>) : T");

        assertNotOverridable(
                "fun <T1, X : Array<out T1>> a(a : Array<*>) : T1",
                "fun <T, Y : Array<out T>> a(a : Array<in T>) : T");

    }

    private void assertOverridable(String superFun, String subFun) {
        assertOverridabilityRelation(superFun, subFun, false);
    }

    private void assertNotOverridable(String superFun, String subFun) {
        assertOverridabilityRelation(superFun, subFun, true);
    }

    private void assertOverridabilityRelation(String superFun, String subFun, boolean expectedIsError) {
        FunctionDescriptor a = makeFunction(superFun);
        FunctionDescriptor b = makeFunction(subFun);
        OverridingUtil.OverrideCompatibilityInfo overridableWith = OverridingUtil.DEFAULT.isOverridableBy(a, b, null);
        assertEquals(
                overridableWith.getDebugMessage(),
                expectedIsError,
                overridableWith.getResult() != OverridingUtil.OverrideCompatibilityInfo.Result.OVERRIDABLE
        );
    }

    private FunctionDescriptor makeFunction(String funDecl) {
        KtNamedFunction function = KtPsiFactoryKt.KtPsiFactory(getProject()).createFunction(funDecl);
        LexicalScope scope = TypeTestUtilsKt.builtInPackageAsLexicalScope(module);
        return functionDescriptorResolver.resolveFunctionDescriptor(
                module, scope, function, KotlinTestUtils.DUMMY_TRACE, DataFlowInfoFactory.EMPTY
        );
    }
}
