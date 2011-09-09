package org.jetbrains.jet.lang.parsing;

/**
* @author abreslav
*/
public class LastBefore extends AbstractTokenStreamPattern {
    private final boolean dontStopRightAfterOccurrence;
    private final TokenStreamPredicate lookFor;
    private final TokenStreamPredicate stopAt;

    private boolean previousLookForResult;

    public LastBefore(TokenStreamPredicate lookFor, TokenStreamPredicate stopAt, boolean dontStopRightAfterOccurrence) {
        this.lookFor = lookFor;
        this.stopAt = stopAt;
        this.dontStopRightAfterOccurrence = dontStopRightAfterOccurrence;
    }

    public LastBefore(TokenStreamPredicate lookFor, TokenStreamPredicate stopAt) {
        this(lookFor, stopAt, false);
    }

    @Override
    public boolean processToken(int offset, boolean topLevel) {
        boolean lookForResult = lookFor.matching(topLevel);
        if (lookForResult) {
            lastOccurrence = offset;
        }
        if (stopAt.matching(topLevel)) {
            if (topLevel
                && (!dontStopRightAfterOccurrence
                    || !previousLookForResult)) return true;
        }
        previousLookForResult = lookForResult;
        return false;
    }
}
