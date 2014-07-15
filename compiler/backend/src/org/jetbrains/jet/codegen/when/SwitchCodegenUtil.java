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

package org.jetbrains.jet.codegen.when;

import kotlin.Function1;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.codegen.ExpressionCodegen;
import org.jetbrains.jet.codegen.binding.CodegenBinding;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.constants.CompileTimeConstant;
import org.jetbrains.jet.lang.resolve.constants.IntegerValueConstant;
import org.jetbrains.org.objectweb.asm.Type;

import java.util.ArrayList;
import java.util.List;

public class SwitchCodegenUtil {
    public static boolean canSwitchBeUsedIn(
            @NotNull JetWhenExpression expression,
            @NotNull Type subjectType,
            @NotNull BindingContext bindingContext
    ) {
        // *switch opcode can be used if each item is enum entry or integral constant
        // in case of enum CodegenAnnotationVisitor should put mappings into bindingContext
        if (bindingContext.get(CodegenBinding.MAPPING_FOR_WHEN_BY_ENUM, expression) != null) {
            return true;
        }

        int typeSort = subjectType.getSort();

        if (typeSort != Type.INT && typeSort != Type.CHAR && typeSort != Type.SHORT && typeSort != Type.BYTE) {
            return false;
        }

        return checkAllItemsAreConstantsSatisfying(expression, bindingContext, new Function1<CompileTimeConstant, Boolean>() {
            @Override
            public Boolean invoke(
                    @NotNull CompileTimeConstant constant
            ) {
                return constant instanceof IntegerValueConstant;
            }
        });
    }

    public static boolean checkAllItemsAreConstantsSatisfying(
            @NotNull JetWhenExpression expression,
            @NotNull BindingContext bindingContext,
            Function1<CompileTimeConstant, Boolean> predicate
    ) {
        for (JetWhenEntry entry : expression.getEntries()) {
            for (JetWhenCondition condition : entry.getConditions()) {
                if (!(condition instanceof JetWhenConditionWithExpression)) {
                    return false;
                }

                // ensure that expression is constant
                JetExpression patternExpression = ((JetWhenConditionWithExpression) condition).getExpression();

                assert patternExpression != null : "expression in when should not be null";

                CompileTimeConstant constant = ExpressionCodegen.getCompileTimeConstant(patternExpression, bindingContext);
                if (constant == null || !predicate.invoke(constant)) {
                    return false;
                }
            }
        }

        return true;
    }

    @NotNull
    public static Iterable<CompileTimeConstant> getAllConstants(
            @NotNull JetWhenExpression expression,
            @NotNull BindingContext bindingContext
    ) {
        List<CompileTimeConstant> result = new ArrayList<CompileTimeConstant>();

        for (JetWhenEntry entry : expression.getEntries()) {
            addConstantsFromEntry(result, entry, bindingContext);
        }

        return result;
    }

    private static void addConstantsFromEntry(
            @NotNull List<CompileTimeConstant> result,
            @NotNull JetWhenEntry entry,
            @NotNull BindingContext bindingContext
    ) {
        for (JetWhenCondition condition : entry.getConditions()) {
            JetExpression patternExpression = ((JetWhenConditionWithExpression) condition).getExpression();

            assert patternExpression != null : "expression in when should not be null";
            result.add(ExpressionCodegen.getCompileTimeConstant(patternExpression, bindingContext));
        }
    }

    @NotNull
    public static Iterable<CompileTimeConstant> getConstantsFromEntry(
            @NotNull JetWhenEntry entry,
            @NotNull BindingContext bindingContext
    ) {
        List<CompileTimeConstant> result = new ArrayList<CompileTimeConstant>();
        addConstantsFromEntry(result, entry, bindingContext);
        return result;
    }

    @NotNull
    public static SwitchCodegen buildAppropriateSwitchCodegen(
            @NotNull JetWhenExpression expression,
            boolean isStatement,
            @NotNull ExpressionCodegen codegen
    ) {
        WhenByEnumsMapping mapping = codegen.getBindingContext().get(CodegenBinding.MAPPING_FOR_WHEN_BY_ENUM, expression);

        if (mapping != null) {
            return new EnumSwitchCodegen(expression, isStatement, codegen, mapping);
        }

        return new IntegralConstantsSwitchCodegen(expression, isStatement, codegen);
    }
}
