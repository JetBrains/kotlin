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
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.KtNodeTypes;
import org.jetbrains.kotlin.parsing.KotlinParsing;

import java.util.Collections;
import java.util.List;

public class KtConstructorDelegationCall extends KtElementImpl implements KtCallElement {
    public KtConstructorDelegationCall(@NotNull ASTNode node) {
        super(node);
    }

    @Override
    @Nullable
    public KtValueArgumentList getValueArgumentList() {
        return (KtValueArgumentList) findChildByType(KtNodeTypes.VALUE_ARGUMENT_LIST);
    }

    @Override
    @NotNull
    public List<? extends ValueArgument> getValueArguments() {
        KtValueArgumentList list = getValueArgumentList();
        return list != null ? list.getArguments() : Collections.<KtValueArgument>emptyList();
    }

    @NotNull
    @Override
    public List<KtLambdaArgument> getLambdaArguments() {
        return Collections.emptyList();
    }

    @NotNull
    @Override
    public List<KtTypeProjection> getTypeArguments() {
        return Collections.emptyList();
    }

    @Override
    public KtTypeArgumentList getTypeArgumentList() {
        return null;
    }

    @Nullable
    @Override
    public KtConstructorDelegationReferenceExpression getCalleeExpression() {
        return findChildByClass(KtConstructorDelegationReferenceExpression.class);
    }

    /**
     * @return true if this delegation call is not present in the source code. Note that we always parse delegation calls
     * for secondary constructors, even if there's no explicit call in the source (see {@link KotlinParsing#parseSecondaryConstructor}).
     *
     *     class Foo {
     *         constructor(name: String)   // <--- implicit constructor delegation call (empty element after RPAR)
     *     }
     */
    public boolean isImplicit() {
        KtConstructorDelegationReferenceExpression callee = getCalleeExpression();
        return callee != null && callee.getFirstChild() == null;
    }

    public boolean isCallToThis() {
        KtConstructorDelegationReferenceExpression callee = getCalleeExpression();
        return callee != null && callee.isThis();
    }
}
