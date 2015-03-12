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

import org.jetbrains.kotlin.builtins.KotlinBuiltIns;
import org.jetbrains.kotlin.cli.jvm.compiler.JetCoreEnvironment;
import org.jetbrains.kotlin.descriptors.FunctionDescriptor;
import org.jetbrains.kotlin.descriptors.ModuleDescriptor;
import org.jetbrains.kotlin.di.InjectorForTests;
import org.jetbrains.kotlin.psi.JetNamedFunction;
import org.jetbrains.kotlin.resolve.FunctionDescriptorResolver;
import org.jetbrains.kotlin.resolve.OverridingUtil;
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowInfo;
import org.jetbrains.kotlin.test.ConfigurationKind;
import org.jetbrains.kotlin.test.JetLiteFixture;
import org.jetbrains.kotlin.test.JetTestUtils;

import static org.jetbrains.kotlin.psi.PsiPackage.JetPsiFactory;

public class JetOverridingTest extends JetLiteFixture {

    private final ModuleDescriptor root = JetTestUtils.createEmptyModule("<test_root>");
    private FunctionDescriptorResolver functionDescriptorResolver;

    @Override
    protected JetCoreEnvironment createEnvironment() {
        return createEnvironmentWithMockJdk(ConfigurationKind.ALL);
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
        InjectorForTests injector = new InjectorForTests(getProject(), root);
        functionDescriptorResolver = injector.getFunctionDescriptorResolver();
    }

    @Override
    protected void tearDown() throws Exception {
        functionDescriptorResolver = null;
        super.tearDown();
    }

    public void testBasic() throws Exception {
        assertOverridable(
                "fun a() : Int",
                "fun a() : Int");

        assertOverridable(
                "fun a<T1>() : T1",
                "fun a<T>() : T");

        assertOverridable(
                "fun a<T1>(a : T1) : T1",
                "fun a<T>(a : T) : T");

        assertOverridable(
                "fun a<T1, X : T1>(a : T1) : T1",
                "fun a<T, Y : T>(a : T) : T");

        assertOverridable(
                "fun a<T1, X : T1>(a : T1) : T1",
                "fun a<T, Y : T>(a : T) : Y");

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
                "fun a<T>(a : Int) : Int",
                "fun a(a : Int) : Int");

        assertNotOverridable(
                "fun a<T1, X : T1>(a : T1) : T1",
                "fun a<T, Y>(a : T) : T");

        assertNotOverridable(
                "fun a<T1, X : T1>(a : T1) : T1",
                "fun a<T, Y : T>(a : Y) : T");

        assertOverridable(
                "fun a<T1, X : T1>(a : T1) : X",
                "fun a<T, Y : T>(a : T) : T");

        assertOverridable(
                "fun a<T1, X : Array<out T1>>(a : Array<in T1>) : T1",
                "fun a<T, Y : Array<out T>>(a : Array<in T>) : T");

        assertNotOverridable(
                "fun a<T1, X : Array<T1>>(a : Array<in T1>) : T1",
                "fun a<T, Y : Array<out T>>(a : Array<in T>) : T");

        assertNotOverridable(
                "fun a<T1, X : Array<out T1>>(a : Array<in T1>) : T1",
                "fun a<T, Y : Array<in T>>(a : Array<in T>) : T");

        assertNotOverridable(
                "fun a<T1, X : Array<out T1>>(a : Array<in T1>) : T1",
                "fun a<T, Y : Array<*>>(a : Array<in T>) : T");

        assertNotOverridable(
                "fun a<T1, X : Array<out T1>>(a : Array<in T1>) : T1",
                "fun a<T, Y : Array<out T>>(a : Array<out T>) : T");

        assertNotOverridable(
                "fun a<T1, X : Array<out T1>>(a : Array<*>) : T1",
                "fun a<T, Y : Array<out T>>(a : Array<in T>) : T");

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
        OverridingUtil.OverrideCompatibilityInfo overridableWith = OverridingUtil.DEFAULT.isOverridableBy(a, b);
        assertEquals(overridableWith.getMessage(), expectedIsError, overridableWith.getResult() != OverridingUtil.OverrideCompatibilityInfo.Result.OVERRIDABLE);
    }

    private FunctionDescriptor makeFunction(String funDecl) {
        JetNamedFunction function = JetPsiFactory(getProject()).createFunction(funDecl);
        return functionDescriptorResolver.resolveFunctionDescriptor(root, KotlinBuiltIns.getInstance().getBuiltInsPackageScope(), function,
                                                            JetTestUtils.DUMMY_TRACE, DataFlowInfo.EMPTY);
    }
}
