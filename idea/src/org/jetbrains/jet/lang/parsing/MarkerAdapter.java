package org.jetbrains.jet.lang.parsing;

import com.intellij.lang.PsiBuilder;
import com.intellij.lang.WhitespacesAndCommentsBinder;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.Nullable;

/**
 * @author abreslav
 */
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
