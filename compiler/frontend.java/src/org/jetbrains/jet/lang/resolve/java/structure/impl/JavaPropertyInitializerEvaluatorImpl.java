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

package org.jetbrains.jet.lang.resolve.java.structure.impl;

import com.intellij.psi.PsiExpression;
import com.intellij.psi.impl.JavaConstantExpressionEvaluator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.PropertyDescriptor;
import org.jetbrains.jet.lang.evaluate.ConstantExpressionEvaluator;
import org.jetbrains.jet.lang.resolve.constants.CompileTimeConstant;
import org.jetbrains.jet.lang.resolve.constants.ConstantsPackage;
import org.jetbrains.jet.lang.resolve.java.structure.JavaField;
import org.jetbrains.jet.lang.resolve.java.structure.JavaPropertyInitializerEvaluator;

public class JavaPropertyInitializerEvaluatorImpl implements JavaPropertyInitializerEvaluator {
    @Nullable
    @Override
    public CompileTimeConstant<?> getInitializerConstant(@NotNull JavaField field, @NotNull PropertyDescriptor descriptor) {
        PsiExpression initializer = ((JavaFieldImpl) field).getInitializer();
        Object evaluatedExpression = JavaConstantExpressionEvaluator.computeConstantExpression(initializer, false);
        if (evaluatedExpression != null) {
            return ConstantsPackage.createCompileTimeConstant(
                    evaluatedExpression,
                    ConstantExpressionEvaluator.object$.isPropertyCompileTimeConstant(descriptor),
                    false,
                    true,
                    descriptor.getType());
        }
        return null;
    }
}
