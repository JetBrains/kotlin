/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin;

import com.intellij.lang.java.JavaParserDefinition;
import com.intellij.openapi.fileTypes.PlainTextParserDefinition;
import com.intellij.psi.TokenType;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.kotlin.psi.stubs.elements.KtFileElementType;

@SuppressWarnings({"WeakerAccess", "unused"}) // Let all static identifiers be public as well as corresponding elements
public class CommonTokens {
    public final static IElementType PLAIN_FILE_ELEMENT_TYPE = new PlainTextParserDefinition().getFileNodeType();
    public final static IElementType JAVA_FILE = JavaParserDefinition.JAVA_FILE;
    public final static IElementType FILE_ELEMENT_TYPE =  KtFileElementType.INSTANCE;

    public final static IElementType WHITE_SPACE = TokenType.WHITE_SPACE;
    public final static IElementType BAD_CHARACTER = TokenType.BAD_CHARACTER;
    public final static IElementType NEW_LINE_INDENT = TokenType.NEW_LINE_INDENT;
    public final static IElementType ERROR_ELEMENT = TokenType.ERROR_ELEMENT;
    public final static IElementType CODE_FRAGMENT = TokenType.CODE_FRAGMENT;
    public final static IElementType DUMMY_HOLDER = TokenType.DUMMY_HOLDER;

    public final static int PLAIN_FILE_ELEMENT_TYPE_INDEX = IElementType.FIRST_TOKEN_INDEX;
    public final static int JAVA_FILE_INDEX = PLAIN_FILE_ELEMENT_TYPE_INDEX + 1;
    public final static int FILE_ELEMENT_TYPE_INDEX = JAVA_FILE_INDEX + 1;

    public final static int WHITE_SPACE_INDEX = FILE_ELEMENT_TYPE_INDEX + 1;
    public final static int BAD_CHARACTER_INDEX = WHITE_SPACE_INDEX + 1;
    public final static int NEW_LINE_INDENT_INDEX = BAD_CHARACTER_INDEX + 1;
    public final static int ERROR_ELEMENT_INDEX = NEW_LINE_INDENT_INDEX + 1;
    public final static int CODE_FRAGMENT_INDEX = ERROR_ELEMENT_INDEX + 1;
    public final static int DUMMY_HOLDER_INDEX = CODE_FRAGMENT_INDEX + 1;

    static {
        ElementTypeChecker.checkExplicitStaticIndexesMatchImplicit(CommonTokens.class);
    }
}
