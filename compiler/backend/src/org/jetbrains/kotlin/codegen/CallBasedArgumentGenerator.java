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

import static org.jetbrains.kotlin.codegen.StackValue.*;

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
        callGenerator.putValueIfNeeded(valueParameterTypes.get(i), createDefaultValue(valueParameterTypes.get(i)), ValueKind.DEFAULT_PARAMETER, i);
    }

    @Override
    protected void generateVararg(int i, @NotNull VarargValueArgument argument) {
        ValueParameterDescriptor parameter = valueParameters.get(i);
        // Upper bound for type of vararg parameter should always have a form of 'Array<out T>',
        // while its lower bound may be Nothing-typed after approximation
        StackValue lazyVararg = codegen.genVarargs(argument, FlexibleTypesKt.upperIfFlexible(parameter.getType()));
        callGenerator.putValueIfNeeded(valueParameterTypes.get(i), lazyVararg, ValueKind.GENERAL_VARARG, i);
    }

    @Override
    protected void generateDefaultJava(int i, @NotNull DefaultValueArgument argument) {
        StackValue argumentValue = StackValueKt.findJavaDefaultArgumentValue(
                valueParameters.get(i),
                valueParameterTypes.get(i),
                codegen.typeMapper
        );

        callGenerator.putValueIfNeeded(valueParameterTypes.get(i), argumentValue);
    }

    @Override
    protected void reorderArgumentsIfNeeded(@NotNull List<ArgumentAndDeclIndex> actualArgsWithDeclIndex) {
        callGenerator.reorderArgumentsIfNeeded(actualArgsWithDeclIndex, valueParameterTypes);
    }
}
