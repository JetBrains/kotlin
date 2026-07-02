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

package org.jetbrains.kotlin.psi;

import org.jetbrains.annotations.NotNull;

/**
 * Represents an expression built around an operation sign, such as a unary, binary, or "is"/"as" expression.
 *
 * <p>The operation sign itself (for example, {@code +}, {@code !}, or {@code in}) is exposed as a
 * {@link KtOperationReferenceExpression}, which can be resolved to the operator or conversion function it stands for.
 *
 * @see KtBinaryExpression
 * @see KtUnaryExpression
 */
public interface KtOperationExpression extends KtExpression {
    /**
     * Returns the operation sign of this expression as a reference that can be resolved to the corresponding function.
     */
    @NotNull
    KtSimpleNameExpression getOperationReference();
}
