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

import com.intellij.lang.ASTNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.KtNodeTypes;
import org.jetbrains.kotlin.name.Name;

public class KtValueArgumentName extends KtElementImpl implements ValueArgumentName {
    public KtValueArgumentName(@NotNull ASTNode node) {
        super(node);
    }

    @Override
    @NotNull
    public KtSimpleNameExpression getReferenceExpression() {
        return (KtSimpleNameExpression) findChildByType(KtNodeTypes.REFERENCE_EXPRESSION);
    }

    @NotNull
    @Override
    public Name getAsName() {
        return getReferenceExpression().getReferencedNameAsName();
    }
}
