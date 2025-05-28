/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.parsing;

import com.intellij.psi.tree.IElementType;

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
     * Decides if the combination of open bracket counts makes a "top level position"
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
