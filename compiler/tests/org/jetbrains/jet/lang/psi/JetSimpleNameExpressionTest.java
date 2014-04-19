/*
 * Copyright 2010-2014 JetBrains s.r.o.
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

package org.jetbrains.jet.lang.psi;

import org.junit.Assert;
import org.jetbrains.jet.JetLiteFixture;
import org.jetbrains.jet.cli.jvm.compiler.JetCoreEnvironment;
import org.jetbrains.jet.config.CompilerConfiguration;

public class JetSimpleNameExpressionTest extends JetLiteFixture {
    public void testGetReceiverExpressionIdentifier() {
        // Binary Expressions
        JetBinaryExpression expr1 = JetPsiFactory.createBinaryExpression(this.getProject(), "1", "+", "2");
        Assert.assertEquals("1", expr1.getOperationReference().getReceiverExpression().getText());
        JetBinaryExpression expr2 = JetPsiFactory.createBinaryExpression(this.getProject(), "1", "in", "array(1)");
        Assert.assertEquals("array(1)", expr2.getOperationReference().getReceiverExpression().getText());
        JetBinaryExpression expr3 = JetPsiFactory.createBinaryExpression(this.getProject(), "1", "to", "2");
        Assert.assertEquals("1", expr3.getOperationReference().getReceiverExpression().getText());
    }
    @Override
    protected JetCoreEnvironment createEnvironment() {
        return JetCoreEnvironment.createForTests(getTestRootDisposable(), new CompilerConfiguration());
    }
}
