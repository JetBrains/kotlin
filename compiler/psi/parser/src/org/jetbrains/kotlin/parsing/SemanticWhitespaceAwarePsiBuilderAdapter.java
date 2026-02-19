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

package org.jetbrains.kotlin.parsing;

import com.intellij.lang.impl.PsiBuilderAdapter;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;

public class SemanticWhitespaceAwarePsiBuilderAdapter extends PsiBuilderAdapter implements SemanticWhitespaceAwarePsiBuilder {

    private final SemanticWhitespaceAwarePsiBuilder myBuilder;

    public SemanticWhitespaceAwarePsiBuilderAdapter(SemanticWhitespaceAwarePsiBuilder builder) {
        super(builder);
        this.myBuilder = builder;
    }

    @Override
    public boolean newlineBeforeCurrentToken() {
        return myBuilder.newlineBeforeCurrentToken();
    }

    @Override
    public void disableNewlines() {
        myBuilder.disableNewlines();
    }

    @Override
    public void enableNewlines() {
        myBuilder.enableNewlines();
    }

    @Override
    public void restoreNewlinesState() {
        myBuilder.restoreNewlinesState();
    }

    @Override
    public void restoreJoiningComplexTokensState() {
        myBuilder.restoreJoiningComplexTokensState();
    }

    @Override
    public void enableJoiningComplexTokens() {
        myBuilder.enableJoiningComplexTokens();
    }

    @Override
    public void disableJoiningComplexTokens() {
        myBuilder.disableJoiningComplexTokens();
    }

    @Override
    public boolean isWhitespaceOrComment(@NotNull IElementType elementType) {
        return myBuilder.isWhitespaceOrComment(elementType);
    }
}
