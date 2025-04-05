/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin;

import com.intellij.psi.TokenType;
import com.intellij.psi.tree.IElementType;

@SuppressWarnings({"WeakerAccess", "unused"}) // Let all static identifiers be public as well as corresponding elements
public class CommonTokens {
    public final static IElementType WHITE_SPACE = TokenType.WHITE_SPACE;
    public final static IElementType BAD_CHARACTER = TokenType.BAD_CHARACTER;
    public final static IElementType NEW_LINE_INDENT = TokenType.NEW_LINE_INDENT;
    public final static IElementType ERROR_ELEMENT = TokenType.ERROR_ELEMENT;
    public final static IElementType CODE_FRAGMENT = TokenType.CODE_FRAGMENT;
    public final static IElementType DUMMY_HOLDER = TokenType.DUMMY_HOLDER;

    private final static int START_OFFSET = 3; // The specific value is calculated based on already initialized internal elements

    public final static int WHITE_SPACE_INDEX = START_OFFSET + 1;
    public final static int BAD_CHARACTER_INDEX = WHITE_SPACE_INDEX + 1;
    public final static int NEW_LINE_INDENT_INDEX = BAD_CHARACTER_INDEX + 1;
    public final static int ERROR_ELEMENT_INDEX = NEW_LINE_INDENT_INDEX + 1;
    public final static int CODE_FRAGMENT_INDEX = ERROR_ELEMENT_INDEX + 1;
    public final static int DUMMY_HOLDER_INDEX = CODE_FRAGMENT_INDEX + 1;

    static {
        ElementTypeChecker.checkExplicitStaticIndexesMatchImplicit(CommonTokens.class);
    }
}
