package org.jetbrains.jet.lang.parsing;

/**
 * @author abreslav
 */
public interface TokenStreamPredicate {
    boolean matching(boolean topLevel);

    TokenStreamPredicate or(TokenStreamPredicate other);
}
