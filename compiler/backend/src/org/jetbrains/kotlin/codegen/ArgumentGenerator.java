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
import org.jetbrains.jet.lang.resolve.calls.model.DefaultValueArgument;
import org.jetbrains.jet.lang.resolve.calls.model.ExpressionValueArgument;
import org.jetbrains.jet.lang.resolve.calls.model.ResolvedValueArgument;
import org.jetbrains.jet.lang.resolve.calls.model.VarargValueArgument;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public abstract class ArgumentGenerator {
    /**
     * @return a {@code List} of bit masks of default arguments that should be passed as last arguments to $default method, if there were
     * any default arguments, or an empty {@code List} if there were none
     */
    @NotNull
    public List<Integer> generate(@NotNull List<ResolvedValueArgument> valueArguments) {
        List<Integer> masks = new ArrayList<Integer>(1);
        boolean maskIsNeeded = false;
        int mask = 0;
        int n = valueArguments.size();
        for (int i = 0; i < n; i++) {
            if (i != 0 && i % Integer.SIZE == 0) {
                masks.add(mask);
                mask = 0;
            }
            ResolvedValueArgument argument = valueArguments.get(i);
            if (argument instanceof ExpressionValueArgument) {
                generateExpression(i, (ExpressionValueArgument) argument);
            }
            else if (argument instanceof DefaultValueArgument) {
                maskIsNeeded = true;
                mask |= 1 << (i % Integer.SIZE);
                generateDefault(i, (DefaultValueArgument) argument);
            }
            else if (argument instanceof VarargValueArgument) {
                generateVararg(i, (VarargValueArgument) argument);
            }
            else {
                generateOther(i, argument);
            }
        }

        if (!maskIsNeeded) {
            return Collections.emptyList();
        }

        masks.add(mask);
        return masks;
    }

    protected void generateExpression(int i, @NotNull ExpressionValueArgument argument) {
        throw new UnsupportedOperationException("Unsupported expression value argument #" + i + ": " + argument);
    }

    protected void generateDefault(int i, @NotNull DefaultValueArgument argument) {
        throw new UnsupportedOperationException("Unsupported default value argument #" + i + ": " + argument);
    }

    protected void generateVararg(int i, @NotNull VarargValueArgument argument) {
        throw new UnsupportedOperationException("Unsupported vararg value argument #" + i + ": " + argument);
    }

    @SuppressWarnings("MethodMayBeStatic") // is supposed to be overridden
    protected void generateOther(int i, @NotNull ResolvedValueArgument argument) {
        throw new UnsupportedOperationException("Unsupported value argument #" + i + ": " + argument);
    }
}
