/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.codegen;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor;
import org.jetbrains.kotlin.psi.KtExpression;
import org.jetbrains.kotlin.psi.ValueArgument;
import org.jetbrains.kotlin.resolve.calls.model.DefaultValueArgument;
import org.jetbrains.kotlin.resolve.calls.model.ExpressionValueArgument;
import org.jetbrains.kotlin.resolve.calls.model.VarargValueArgument;
import org.jetbrains.kotlin.types.FlexibleTypesKt;
import org.jetbrains.org.objectweb.asm.Type;

import java.util.List;

import static org.jetbrains.kotlin.codegen.StackValue.createDefaultValue;

public class CallBasedArgumentGenerator extends ArgumentGenerator {
    private final ExpressionCodegen codegen;
    private final CallGenerator callGenerator;
    private final List<ValueParameterDescriptor> valueParameters;
    private final List<Type> valueParameterTypes;

    public CallBasedArgumentGenerator(
            @NotNull ExpressionCodegen codegen,
            @NotNull CallGenerator callGenerator,
            @NotNull List<ValueParameterDescriptor> valueParameters,
            @NotNull List<Type> valueParameterTypes
    ) {
        this.codegen = codegen;
        this.callGenerator = callGenerator;
        this.valueParameters = valueParameters;
        this.valueParameterTypes = valueParameterTypes;

        assert valueParameters.size() == valueParameterTypes.size() :
                "Value parameters and their types mismatch in sizes: " + valueParameters.size() + " != " + valueParameterTypes.size();
    }

    @Override
    protected void generateExpression(int i, @NotNull ExpressionValueArgument argument) {
        ValueParameterDescriptor parameter = valueParameters.get(i);
        Type type = valueParameterTypes.get(i);
        ValueArgument valueArgument = argument.getValueArgument();
        assert valueArgument != null;
        KtExpression argumentExpression = valueArgument.getArgumentExpression();
        assert argumentExpression != null : valueArgument.asElement().getText();
        callGenerator.genValueAndPut(parameter, argumentExpression, type, i);
    }

    @Override
    protected void generateDefault(int i, @NotNull DefaultValueArgument argument) {
        callGenerator.putValueIfNeeded(
                getJvmKotlinType(valueParameterTypes, valueParameters, i),
                createDefaultValue(valueParameterTypes.get(i)),
                ValueKind.DEFAULT_PARAMETER,
                i
        );
    }

    @Override
    protected void generateVararg(int i, @NotNull VarargValueArgument argument) {
        ValueParameterDescriptor parameter = valueParameters.get(i);
        // Upper bound for type of vararg parameter should always have a form of 'Array<out T>',
        // while its lower bound may be Nothing-typed after approximation
        StackValue lazyVararg = codegen.genVarargs(argument, FlexibleTypesKt.upperIfFlexible(parameter.getType()));
        callGenerator.putValueIfNeeded(getJvmKotlinType(valueParameterTypes, valueParameters, i), lazyVararg, ValueKind.GENERAL_VARARG, i);
    }

    @Override
    protected void generateDefaultJava(int i, @NotNull DefaultValueArgument argument) {
        StackValue argumentValue = StackValueKt.findJavaDefaultArgumentValue(
                valueParameters.get(i),
                valueParameterTypes.get(i),
                codegen.typeMapper
        );

        callGenerator.putValueIfNeeded(getJvmKotlinType(valueParameterTypes, valueParameters, i), argumentValue);
    }

    @Override
    protected void reorderArgumentsIfNeeded(@NotNull List<ArgumentAndDeclIndex> actualArgsWithDeclIndex) {
        callGenerator.reorderArgumentsIfNeeded(actualArgsWithDeclIndex, valueParameterTypes);
    }

    @NotNull
    private static JvmKotlinType getJvmKotlinType(
            @NotNull List<Type> valueParameterTypes,
            @NotNull List<ValueParameterDescriptor> valueParameters, int i
    ) {
        return new JvmKotlinType(valueParameterTypes.get(i), valueParameters.get(i).getOriginal().getType());
    }
}
