/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

package org.jetbrains.kotlin.codegen;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.org.objectweb.asm.MethodVisitor;
import org.jetbrains.org.objectweb.asm.Opcodes;
import org.jetbrains.org.objectweb.asm.tree.LocalVariableNode;
import org.jetbrains.org.objectweb.asm.tree.MethodNode;
import org.jetbrains.org.objectweb.asm.util.Textifier;
import org.jetbrains.org.objectweb.asm.util.TraceMethodVisitor;

import java.util.ArrayList;
import java.util.List;

import static org.jetbrains.kotlin.codegen.inline.InlineCodegenUtilsKt.getNodeText;
import static org.jetbrains.kotlin.codegen.inline.InlineCodegenUtilsKt.wrapWithMaxLocalCalc;

public abstract class TransformationMethodVisitor extends MethodVisitor {

    private final MethodNode methodNode;
    private final MethodVisitor delegate;

    public TransformationMethodVisitor(
            @NotNull MethodVisitor delegate,
            int access,
            @NotNull String name,
            @NotNull String desc,
            @Nullable String signature,
            @Nullable String[] exceptions
    ) {
        super(Opcodes.ASM5);
        this.delegate = delegate;
        this.methodNode = new MethodNode(access, name, desc, signature, exceptions);
        this.methodNode.localVariables = new ArrayList<>(5);
        this.mv = wrapWithMaxLocalCalc(methodNode);
    }

    @Override
    public void visitEnd() {
        // force mv to calculate maxStack/maxLocals in case it didn't yet done
        if (methodNode.maxLocals <= 0 || methodNode.maxStack <= 0) {
            mv.visitMaxs(-1, -1);
        }

        super.visitEnd();

        try {
            if (shouldBeTransformed(methodNode)) {
                performTransformations(methodNode);
            }

            methodNode.accept(new EndIgnoringMethodVisitorDecorator(Opcodes.ASM5, delegate));


            // In case of empty instructions list MethodNode.accept doesn't call visitLocalVariables of delegate
            // So we just do it here
            if (methodNode.instructions.size() == 0
                // MethodNode does not create a list of variables for abstract methods, so we would get NPE in accept() instead
                && (!(delegate instanceof MethodNode) || methodNode.localVariables != null)
            ) {
                List<LocalVariableNode> localVariables = methodNode.localVariables;
                // visits local variables
                int n = localVariables == null ? 0 : localVariables.size();
                for (int i = 0; i < n; ++i) {
                    localVariables.get(i).accept(delegate);
                }
            }

            delegate.visitEnd();
        }
        catch (Throwable t) {
            throw new CompilationException("Couldn't transform method node:\n" + getNodeText(methodNode), t, null);
        }
    }

    protected abstract void performTransformations(@NotNull MethodNode methodNode);

    /**
     * You can use it when you need to ignore visit end
     */
    private static class EndIgnoringMethodVisitorDecorator extends MethodVisitor {
        public EndIgnoringMethodVisitorDecorator(int api, @NotNull MethodVisitor mv) {
            super(api, mv);
        }

        @Override
        public void visitEnd() {

        }
    }

    @Nullable
    public TraceMethodVisitor getTraceMethodVisitorIfPossible() {
        TraceMethodVisitor traceMethodVisitor = new TraceMethodVisitor(new Textifier());
        try {
            methodNode.accept(traceMethodVisitor);
        }
        catch (Throwable e) {
            return null;
        }

        return traceMethodVisitor;
    }

    private static boolean shouldBeTransformed(@NotNull MethodNode node) {
        return node.instructions.size() > 0;
    }
}
