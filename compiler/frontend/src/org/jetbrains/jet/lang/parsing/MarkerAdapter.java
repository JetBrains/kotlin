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

package org.jetbrains.jet.lang.parsing;

import com.intellij.lang.PsiBuilder;
import com.intellij.lang.WhitespacesAndCommentsBinder;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.Nullable;

public class MarkerAdapter implements PsiBuilder.Marker {

    private final PsiBuilder.Marker delegate;

    public MarkerAdapter(PsiBuilder.Marker delegate) {
        this.delegate = delegate;
    }

    @Override
    public PsiBuilder.Marker precede() {
        return delegate.precede();
    }

    @Override
    public void drop() {
        delegate.drop();
    }

    @Override
    public void rollbackTo() {
        delegate.rollbackTo();
    }

    @Override
    public void done(IElementType type) {
        delegate.done(type);
    }

    @Override
    public void collapse(IElementType type) {
        delegate.collapse(type);
    }

    @Override
    public void doneBefore(IElementType type, PsiBuilder.Marker before) {
        delegate.doneBefore(type, before);
    }

    @Override
    public void doneBefore(IElementType type, PsiBuilder.Marker before, String errorMessage) {
        delegate.doneBefore(type, before, errorMessage);
    }

    @Override
    public void error(String message) {
        delegate.error(message);
    }

    @Override
    public void errorBefore(String message, PsiBuilder.Marker before) {
        delegate.errorBefore(message, before);
    }

    @Override
    public void setCustomEdgeTokenBinders(@Nullable WhitespacesAndCommentsBinder left, @Nullable WhitespacesAndCommentsBinder right) {
        delegate.setCustomEdgeTokenBinders(left, right);
    }
}
