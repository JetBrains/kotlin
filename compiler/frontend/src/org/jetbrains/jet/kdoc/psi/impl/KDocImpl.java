/*
 * Copyright 2010-2013 JetBrains s.r.o.
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

package org.jetbrains.jet.kdoc.psi.impl;

import com.intellij.lang.Language;
import com.intellij.psi.impl.source.tree.LazyParseablePsiElement;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.kdoc.lexer.KDocTokens;
import org.jetbrains.jet.kdoc.psi.api.KDoc;
import org.jetbrains.jet.lexer.JetTokens;
import org.jetbrains.jet.plugin.JetLanguage;

public class KDocImpl extends LazyParseablePsiElement implements KDoc {
    public KDocImpl(CharSequence buffer) {
        super(KDocTokens.KDOC, buffer);
    }

    @NotNull
    @Override
    public Language getLanguage() {
        return JetLanguage.INSTANCE;
    }

    @Override
    public String toString() {
        return getNode().getElementType().toString();
    }

    @Override
    public IElementType getTokenType() {
        return JetTokens.DOC_COMMENT;
    }
}
