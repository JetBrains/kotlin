package org.jetbrains.jet.lang.parsing;

/**
 * @author abreslav
 */
public abstract class AbstractTokenStreamPredicate implements TokenStreamPredicate {

    @Override
    public TokenStreamPredicate or(final TokenStreamPredicate other) {
        return new AbstractTokenStreamPredicate() {
            @Override
            public boolean matching(boolean topLevel) {
                if (AbstractTokenStreamPredicate.this.matching(topLevel)) return true;
                return other.matching(topLevel);
            }
        };
    }
}
