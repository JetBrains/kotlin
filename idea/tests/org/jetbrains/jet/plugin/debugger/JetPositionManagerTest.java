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

package org.jetbrains.jet.plugin.debugger;

import com.intellij.debugger.PositionManagerFactory;
import org.jetbrains.jet.plugin.PluginTestCaseBase;

/**
 * @author udalov
 */
public class JetPositionManagerTest extends PositionManagerTestCase {
    @Override
    protected String getTestDataPath() {
        return PluginTestCaseBase.getTestDataPathBase() + "/debugger";
    }

    @Override
    protected PositionManagerFactory getPositionManagerFactory() {
        return new JetPositionManagerFactory();
    }

    public void testAnnotation() {
        doTest();
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

    public void testNamespace() {
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
}
