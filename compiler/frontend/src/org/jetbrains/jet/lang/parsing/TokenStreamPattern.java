package org.jetbrains.jet.lang.parsing;

import com.intellij.psi.tree.IElementType;

/**
 * @author abreslav
 */
public interface TokenStreamPattern {
    /**
     * Called on each token
     *
     * @param offset
     * @param topLevel see {@link #isTopLevel(int, int, int, int)}
     * @return <code>true</code> to stop
     */
    boolean processToken(int offset, boolean topLevel);

    /**
     * @return the position where the predicate has matched, -1 if no match was found
     */
    int result();

    /**
     * Decides if the combination of open bracet counts makes a "top level position"
     * Straightforward meaning would be: if all counts are zero, then it's a top level
     */
    boolean isTopLevel(int openAngleBrackets, int openBrackets, int openBraces, int openParentheses);

    /**
     * Called on right parentheses, brackets, braces and angles (>)
     * @param token the closing bracket
     * @return true to stop matching, false to proceed
     */
    boolean handleUnmatchedClosing(IElementType token);
}
