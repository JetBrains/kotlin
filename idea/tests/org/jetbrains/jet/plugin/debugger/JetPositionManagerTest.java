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

package org.jetbrains.jet.plugin.debugger;

import com.intellij.debugger.engine.DebugProcess;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.codegen.ClassBuilderMode;
import org.jetbrains.jet.codegen.binding.CodegenBinding;
import org.jetbrains.jet.codegen.state.GenerationState;
import org.jetbrains.jet.codegen.state.JetTypeMapper;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.resolve.DelegatingBindingTrace;
import org.jetbrains.jet.plugin.PluginTestCaseBase;

import java.util.List;

public class JetPositionManagerTest extends PositionManagerTestCase {
    private final JetPositionManagerFactory jetPositionManagerFactory = new JetPositionManagerFactory();

    @Override
    @NotNull
    protected String getTestDataPath() {
        return PluginTestCaseBase.getTestDataPathBase();
    }

    @Override
    protected String getTestRoot() {
        return "/debugger/";
    }

    @Override
    @NotNull
    protected JetPositionManager createPositionManager(DebugProcess process, List<JetFile> files, GenerationState state) {
        JetPositionManager positionManager = (JetPositionManager) jetPositionManagerFactory.createPositionManager(process);
        assertNotNull(positionManager);

        DelegatingBindingTrace bindingTrace = new DelegatingBindingTrace(state.getBindingContext(), "trace created in JetPositionManagerTest");
        JetTypeMapper typeMapper = new JetTypeMapper(bindingTrace, true, ClassBuilderMode.FULL);
        //noinspection unchecked
        CodegenBinding.initTrace(bindingTrace, files);
        for (JetFile file : files) {
            positionManager.addTypeMapper(file, typeMapper);
        }

        return positionManager;
    }

    public void testMultiFileNamespace() {
        doMultiTest();
    }

    public void testMultiFileSameName() {
        doMultiTest();
    }

    public void testAnonymousFunction() {
        doTest();
    }

    public void testClass() {
        doTest();
    }

    public void testClassObject() {
        doTest();
    }

    public void testEnum() {
        doTest();
    }

    public void testExtensionFunction() {
        doTest();
    }

    public void testInnerClass() {
        doTest();
    }

    public void testLocalFunction() {
        doTest();
    }

    public void test_DefaultPackage() {
        doTest();
    }

    public void testNamespaceOfPackage() {
        doTest();
    }

    public void testObjectDeclaration() {
        doTest();
    }

    public void testObjectExpression() {
        doTest();
    }

    public void testPropertyAccessor() {
        doTest();
    }

    public void testTrait() {
        doTest();
    }

    public void testTwoClasses() {
        doTest();
    }
}
