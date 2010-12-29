package org.jetbrains.jet.lang.parsing;

/**
* @author abreslav
*/
public class FirstBefore extends AbstractTokenStreamPattern {
    private final TokenStreamPredicate lookFor;
    private final TokenStreamPredicate stopAt;

    public FirstBefore(TokenStreamPredicate lookFor, TokenStreamPredicate stopAt) {
        this.lookFor = lookFor;
        this.stopAt = stopAt;
    }

    @Override
    public boolean processToken(int offset, boolean topLevel) {
        if (lookFor.matching(topLevel)) {
            lastOccurrence = offset;
            return true;
        }
        if (stopAt.matching(topLevel)) {
            return true;
        }
        return false;
    }
}
