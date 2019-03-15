/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.codegen;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.codegen.context.MethodContext;
import org.jetbrains.kotlin.codegen.state.GenerationState;
import org.jetbrains.kotlin.codegen.state.KotlinTypeMapper;
import org.jetbrains.kotlin.descriptors.FunctionDescriptor;
import org.jetbrains.kotlin.psi.KtDeclarationWithBody;
import org.jetbrains.kotlin.psi.KtExpression;
import org.jetbrains.kotlin.psi.psiUtil.PsiUtilsKt;
import org.jetbrains.kotlin.resolve.jvm.jvmSignature.JvmMethodGenericSignature;
import org.jetbrains.kotlin.resolve.jvm.jvmSignature.JvmMethodSignature;
import org.jetbrains.org.objectweb.asm.MethodVisitor;

public abstract class FunctionGenerationStrategy {
    public abstract void generateBody(
            @NotNull MethodVisitor mv,
            @NotNull FrameMap frameMap,
            @NotNull JvmMethodSignature signature,
            @NotNull MethodContext context,
            @NotNull MemberCodegen<?> parentCodegen
    );

    public abstract boolean skipNotNullAssertionsForParameters();

    public boolean skipGenericSignature() {
        return false;
    }

    public MethodVisitor wrapMethodVisitor(@NotNull MethodVisitor mv, int access, @NotNull String name, @NotNull String desc) {
        return mv;
    }

    @NotNull
    public JvmMethodGenericSignature mapMethodSignature(
            @NotNull FunctionDescriptor functionDescriptor,
            @NotNull KotlinTypeMapper typeMapper,
            @NotNull OwnerKind contextKind,
            boolean hasSpecialBridge
    ) {
        return typeMapper.mapSignatureWithGeneric(functionDescriptor, contextKind, hasSpecialBridge);
    }

    public static class FunctionDefault extends CodegenBased {
        private final KtDeclarationWithBody declaration;

        public FunctionDefault(
                @NotNull GenerationState state,
                @NotNull KtDeclarationWithBody declaration
        ) {
            super(state);
            this.declaration = declaration;
        }

        @Override
        public void doGenerateBody(@NotNull ExpressionCodegen codegen, @NotNull JvmMethodSignature signature) {
            KtExpression bodyExpression = declaration.getBodyExpression();
            assert bodyExpression != null : "Function has no body: " + PsiUtilsKt.getElementTextWithContext(declaration);
            codegen.returnExpression(bodyExpression);
        }
    }

    public abstract static class CodegenBased extends FunctionGenerationStrategy {
        protected final GenerationState state;

        public CodegenBased(@NotNull GenerationState state) {
            this.state = state;
        }

        @Override
        public final void generateBody(
                @NotNull MethodVisitor mv,
                @NotNull FrameMap frameMap,
                @NotNull JvmMethodSignature signature,
                @NotNull MethodContext context,
                @NotNull MemberCodegen<?> parentCodegen
        ) {
            ExpressionCodegen codegen = new ExpressionCodegen(mv, frameMap, signature.getReturnType(), context, state, parentCodegen);
            doGenerateBody(codegen, signature);
        }

        @Override
        public boolean skipNotNullAssertionsForParameters() {
            // Assume the strategy injects non-null checks for parameters by default
            return false;
        }

        public abstract void doGenerateBody(@NotNull ExpressionCodegen codegen, @NotNull JvmMethodSignature signature);
    }
}
