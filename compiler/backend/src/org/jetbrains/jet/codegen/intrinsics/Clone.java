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

package org.jetbrains.jet.codegen.intrinsics;

import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.codegen.ExpressionCodegen;
import org.jetbrains.jet.codegen.StackValue;
import org.jetbrains.jet.lang.psi.JetElement;
import org.jetbrains.jet.lang.psi.JetExpression;
import org.jetbrains.jet.lang.psi.JetSuperExpression;
import org.jetbrains.jet.lang.resolve.calls.model.ResolvedCall;
import org.jetbrains.jet.lang.resolve.scopes.receivers.ExpressionReceiver;
import org.jetbrains.jet.lang.resolve.scopes.receivers.ReceiverValue;
import org.jetbrains.org.objectweb.asm.Type;
import org.jetbrains.org.objectweb.asm.commons.InstructionAdapter;

import java.util.List;

import static org.jetbrains.jet.lang.resolve.calls.callUtil.CallUtilPackage.getResolvedCallWithAssert;
import static org.jetbrains.jet.lang.resolve.java.AsmTypeConstants.OBJECT_TYPE;

public class Clone extends IntrinsicMethod {
    @NotNull
    @Override
    protected Type generateImpl(
            @NotNull ExpressionCodegen codegen,
            @NotNull InstructionAdapter v,
            @NotNull Type returnType,
            @Nullable PsiElement element,
            @Nullable List<JetExpression> arguments,
            @Nullable StackValue receiver
    ) {
        ResolvedCall<?> resolvedCall = getResolvedCallWithAssert(((JetElement) element), codegen.getBindingContext());
        StackValue.receiver(resolvedCall, receiver, codegen, null).put(OBJECT_TYPE, v);
        if (isSuperCall(resolvedCall)) {
            v.invokespecial("java/lang/Object", "clone", "()Ljava/lang/Object;", false);
        }
        else {
            v.invokevirtual("java/lang/Object", "clone", "()Ljava/lang/Object;", false);
        }
        return OBJECT_TYPE;
    }

    private static boolean isSuperCall(@NotNull ResolvedCall<?> resolvedCall) {
        ReceiverValue thisObject = resolvedCall.getThisObject();
        return thisObject instanceof ExpressionReceiver &&
               ((ExpressionReceiver) thisObject).getExpression() instanceof JetSuperExpression;
    }
}
