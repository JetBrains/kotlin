package com.intellij.psi.tree;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * @author abreslav
 */
public class TokenSet {
    public static TokenSet create(IElementType... tokens) {
        return new TokenSet(tokens);
    }

    private final Set<IElementType> tokens = new HashSet<IElementType>();

    public TokenSet(IElementType... tokens) {
        this.tokens.addAll(Arrays.asList(tokens));
    }

    public Set<IElementType> asSet() {
        return tokens;
    }
}
