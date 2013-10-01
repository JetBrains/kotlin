package org.jetbrains.jet.codegen.intrinsics;

import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.asm4.Type;
import org.jetbrains.asm4.commons.InstructionAdapter;
import org.jetbrains.jet.codegen.ExpressionCodegen;
import org.jetbrains.jet.codegen.StackValue;
import org.jetbrains.jet.codegen.state.GenerationState;
import org.jetbrains.jet.lang.psi.JetExpression;

import java.util.List;

public class CopyToArray implements IntrinsicMethod {
    @Override
    public StackValue generate(
            ExpressionCodegen codegen,
            InstructionAdapter v,
            @NotNull Type expectedType,
            @Nullable PsiElement element,
            @Nullable List<JetExpression> arguments,
            @Nullable StackValue receiver,
            @NotNull GenerationState state
    ) {
        assert receiver != null;
        receiver.put(receiver.type, v);
        v.dup();
        v.invokeinterface("java/util/Collection", "size", "()I");

        v.newarray(expectedType.getElementType());
        v.invokeinterface("java/util/Collection", "toArray", "([Ljava/lang/Object;)[Ljava/lang/Object;");

        StackValue.coerce(Type.getType("[Ljava/lang/Object;"), expectedType, v);

        return StackValue.onStack(expectedType);
    }
}
