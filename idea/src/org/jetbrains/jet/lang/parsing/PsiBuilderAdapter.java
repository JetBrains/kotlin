package org.jetbrains.jet.lang.parsing;

import com.intellij.lang.*;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.TokenSet;
import com.intellij.util.diff.FlyweightCapableTreeStructure;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Copied from JavaParserUtil
 *
 * @author abreslav
 */
public class PsiBuilderAdapter implements PsiBuilder {
    protected final PsiBuilder myDelegate;

    public PsiBuilderAdapter(final PsiBuilder delegate) {
        myDelegate = delegate;
    }

    public Project getProject() {
        return myDelegate.getProject();
    }

    public CharSequence getOriginalText() {
        return myDelegate.getOriginalText();
    }

    public void advanceLexer() {
        myDelegate.advanceLexer();
    }

    @Nullable
    public IElementType getTokenType() {
        return myDelegate.getTokenType();
    }

    public void setTokenTypeRemapper(final ITokenTypeRemapper remapper) {
        myDelegate.setTokenTypeRemapper(remapper);
    }

    @Override
    public void setWhitespaceSkippedCallback(WhitespaceSkippedCallback callback) {
        myDelegate.setWhitespaceSkippedCallback(callback);
    }

    @Override
    public void remapCurrentToken(IElementType type) {
        myDelegate.remapCurrentToken(type);
    }

    @Override
    public IElementType lookAhead(int steps) {
        return myDelegate.lookAhead(steps);
    }

    @Nullable
    @NonNls
    public String getTokenText() {
        return myDelegate.getTokenText();
    }

    public int getCurrentOffset() {
        return myDelegate.getCurrentOffset();
    }

    public Marker mark() {
        return myDelegate.mark();
    }

    public void error(final String messageText) {
        myDelegate.error(messageText);
    }

    public boolean eof() {
        return myDelegate.eof();
    }

    public ASTNode getTreeBuilt() {
        return myDelegate.getTreeBuilt();
    }

    public FlyweightCapableTreeStructure<LighterASTNode> getLightTree() {
        return myDelegate.getLightTree();
    }

    public void setDebugMode(final boolean dbgMode) {
        myDelegate.setDebugMode(dbgMode);
    }

    public void enforceCommentTokens(final TokenSet tokens) {
        myDelegate.enforceCommentTokens(tokens);
    }

    @Nullable
    public LighterASTNode getLatestDoneMarker() {
        return myDelegate.getLatestDoneMarker();
    }

    @Nullable
    public <T> T getUserData(@NotNull final Key<T> key) {
        return myDelegate.getUserData(key);
    }

    public <T> void putUserData(@NotNull final Key<T> key, @Nullable final T value) {
        myDelegate.putUserData(key, value);
    }

    @Override
    public <T> T getUserDataUnprotected(@NotNull final Key<T> key) {
        return myDelegate.getUserDataUnprotected(key);
    }

    @Override
    public <T> void putUserDataUnprotected(@NotNull Key<T> key, @Nullable T value) {
        myDelegate.putUserDataUnprotected(key, value);
    }
}
