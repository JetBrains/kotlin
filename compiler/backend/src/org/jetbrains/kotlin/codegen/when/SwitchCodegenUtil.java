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

package org.jetbrains.kotlin.codegen.when;

import kotlin.jvm.functions.Function1;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.codegen.ExpressionCodegen;
import org.jetbrains.kotlin.codegen.binding.CodegenBinding;
import org.jetbrains.kotlin.psi.*;
import org.jetbrains.kotlin.resolve.BindingContext;
import org.jetbrains.kotlin.resolve.constants.ConstantValue;
import org.jetbrains.kotlin.resolve.constants.IntegerValueConstant;
import org.jetbrains.kotlin.resolve.constants.NullValue;
import org.jetbrains.kotlin.resolve.constants.StringValue;
import org.jetbrains.org.objectweb.asm.Type;

import java.util.ArrayList;
import java.util.List;

public class SwitchCodegenUtil {
    public static boolean checkAllItemsAreConstantsSatisfying(
            @NotNull KtWhenExpression expression,
            @NotNull BindingContext bindingContext,
            boolean shouldInlineConstVals,
            Function1<ConstantValue<?>, Boolean> predicate
    ) {
        for (KtWhenEntry entry : expression.getEntries()) {
            for (KtWhenCondition condition : entry.getConditions()) {
                if (!(condition instanceof KtWhenConditionWithExpression)) {
                    return false;
                }

                // ensure that expression is constant
                KtExpression patternExpression = ((KtWhenConditionWithExpression) condition).getExpression();

                if (patternExpression == null) return false;

                ConstantValue<?> constant = ExpressionCodegen.getCompileTimeConstant(patternExpression, bindingContext, shouldInlineConstVals);
                if (constant == null || !predicate.invoke(constant)) {
                    return false;
                }
            }
        }

        return true;
    }

    @NotNull
    public static Iterable<ConstantValue<?>> getAllConstants(
            @NotNull KtWhenExpression expression,
            @NotNull BindingContext bindingContext,
            boolean shouldInlineConstVals
    ) {
        List<ConstantValue<?>> result = new ArrayList<ConstantValue<?>>();

        for (KtWhenEntry entry : expression.getEntries()) {
            addConstantsFromEntry(result, entry, bindingContext, shouldInlineConstVals);
        }

        return result;
    }

    private static void addConstantsFromEntry(
            @NotNull List<ConstantValue<?>> result,
            @NotNull KtWhenEntry entry,
            @NotNull BindingContext bindingContext,
            boolean shouldInlineConstVals
    ) {
        for (KtWhenCondition condition : entry.getConditions()) {
            if (!(condition instanceof KtWhenConditionWithExpression)) continue;

            KtExpression patternExpression = ((KtWhenConditionWithExpression) condition).getExpression();

            assert patternExpression != null : "expression in when should not be null";
            result.add(ExpressionCodegen.getCompileTimeConstant(patternExpression, bindingContext, shouldInlineConstVals));
        }
    }

    @NotNull
    public static Iterable<ConstantValue<?>> getConstantsFromEntry(
            @NotNull KtWhenEntry entry,
            @NotNull BindingContext bindingContext,
            boolean shouldInlineConstVals
    ) {
        List<ConstantValue<?>> result = new ArrayList<ConstantValue<?>>();
        addConstantsFromEntry(result, entry, bindingContext, shouldInlineConstVals);
        return result;
    }

    @Nullable
    public static SwitchCodegen buildAppropriateSwitchCodegenIfPossible(
            @NotNull KtWhenExpression expression,
            boolean isStatement,
            boolean isExhaustive,
            @NotNull ExpressionCodegen codegen
    ) {
        BindingContext bindingContext = codegen.getBindingContext();
        boolean shouldInlineConstVals = codegen.getState().getShouldInlineConstVals();
        if (!isThereConstantEntriesButNulls(expression, bindingContext, shouldInlineConstVals)) {
            return null;
        }

        Type subjectType = codegen.expressionType(expression.getSubjectExpression());

        WhenByEnumsMapping mapping = codegen.getBindingContext().get(CodegenBinding.MAPPING_FOR_WHEN_BY_ENUM, expression);

        if (mapping != null) {
            return new EnumSwitchCodegen(expression, isStatement, isExhaustive, codegen, mapping);
        }

        if (isIntegralConstantsSwitch(expression, subjectType, bindingContext, shouldInlineConstVals)) {
            return new IntegralConstantsSwitchCodegen(expression, isStatement, isExhaustive, codegen);
        }

        if (isStringConstantsSwitch(expression, subjectType, bindingContext, shouldInlineConstVals)) {
            return new StringSwitchCodegen(expression, isStatement, isExhaustive, codegen);
        }

        return null;
    }

    private static boolean isThereConstantEntriesButNulls(
            @NotNull KtWhenExpression expression,
            @NotNull BindingContext bindingContext,
            boolean shouldInlineConstVals
    ) {
        for (ConstantValue<?> constant : getAllConstants(expression, bindingContext, shouldInlineConstVals)) {
            if (constant != null && !(constant instanceof NullValue)) return true;
        }

        return false;
    }

    private static boolean isIntegralConstantsSwitch(
            @NotNull KtWhenExpression expression,
            @NotNull Type subjectType,
            @NotNull BindingContext bindingContext,
            boolean shouldInlineConstVals
    ) {
        int typeSort = subjectType.getSort();

        if (typeSort != Type.INT && typeSort != Type.CHAR && typeSort != Type.SHORT && typeSort != Type.BYTE) {
            return false;
        }

        return checkAllItemsAreConstantsSatisfying(expression, bindingContext, shouldInlineConstVals, new Function1<ConstantValue<?>, Boolean>() {
            @Override
            public Boolean invoke(
                    @NotNull ConstantValue<?> constant
            ) {
                return constant instanceof IntegerValueConstant;
            }
        });
    }

    private static boolean isStringConstantsSwitch(
            @NotNull KtWhenExpression expression,
            @NotNull Type subjectType,
            @NotNull BindingContext bindingContext,
            boolean shouldInlineConstVals
    ) {

        if (!subjectType.getClassName().equals(String.class.getName())) {
            return false;
        }

        return checkAllItemsAreConstantsSatisfying(expression, bindingContext, shouldInlineConstVals, new Function1<ConstantValue<?>, Boolean>() {
            @Override
            public Boolean invoke(
                    @NotNull ConstantValue<?> constant
            ) {
                return constant instanceof StringValue || constant instanceof NullValue;
            }
        });
    }
}
