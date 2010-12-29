package org.jetbrains.jet.lang.parsing;

/**
 * @author abreslav
 */
public interface TokenStreamPattern {
    /**
     * Called on each token
     *
     *
     *
     * @param offset
     * @param topLevel indicates if no brackets (of types () [] {} <>) are currently unmatched
     * @return <code>true</code> to stop
     */
    boolean processToken(int offset, boolean topLevel);

    /**
     * @return the position where the predicate has matched, -1 if no match was found
     */
    int result();
}
