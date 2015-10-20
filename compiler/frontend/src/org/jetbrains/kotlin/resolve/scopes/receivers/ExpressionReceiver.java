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

package org.jetbrains.kotlin.resolve.scopes.receivers;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.psi.KtExpression;
import org.jetbrains.kotlin.types.KotlinType;

public class ExpressionReceiver extends AbstractReceiverValue implements ReceiverValue {

    private final KtExpression expression;

    public ExpressionReceiver(@NotNull KtExpression expression, @NotNull KotlinType type) {
        super(type);
        this.expression = expression;
    }

    @NotNull
    public KtExpression getExpression() {
        return expression;
    }

    @Override
    public String toString() {
        return getType() + " {" + expression + ": " + expression.getText() + "}";
    }
}
