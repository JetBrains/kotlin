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

package org.jetbrains.kotlin.idea.references;

import com.intellij.openapi.util.TextRange;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.psi.KtConstructorDelegationReferenceExpression;

import java.util.Collection;
import java.util.Collections;

public class KtConstructorDelegationReference extends KtSimpleReference<KtConstructorDelegationReferenceExpression> {
    public KtConstructorDelegationReference(KtConstructorDelegationReferenceExpression expression) {
        super(expression);
    }

    @Override
    public TextRange getRangeInElement() {
        return new TextRange(0, getElement().getTextLength());
    }

    @NotNull
    @Override
    public Collection<String> getResolvesByNames() {
        return Collections.emptyList();
    }
}
