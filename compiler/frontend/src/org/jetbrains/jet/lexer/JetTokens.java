/*
 * Copyright 2010-2013 JetBrains s.r.o.
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

package org.jetbrains.jet.lexer;

import com.intellij.psi.TokenType;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.TokenSet;

public interface JetTokens {
    JetToken EOF   = new JetToken("EOF");

    JetToken BLOCK_COMMENT = new JetToken("BLOCK_COMMENT");
    JetToken DOC_COMMENT   = new JetToken("DOC_COMMENT");
    JetToken EOL_COMMENT   = new JetToken("EOL_COMMENT");
    JetToken SHEBANG_COMMENT = new JetToken("SHEBANG_COMMENT");

    IElementType WHITE_SPACE = TokenType.WHITE_SPACE;

    JetToken INTEGER_LITERAL    = new JetToken("INTEGER_LITERAL");
    JetToken FLOAT_LITERAL      = new JetToken("FLOAT_CONSTANT");
    JetToken CHARACTER_LITERAL  = new JetToken("CHARACTER_LITERAL");

    JetToken CLOSING_QUOTE = new JetToken("CLOSING_QUOTE");
    JetToken OPEN_QUOTE = new JetToken("OPEN_QUOTE");
    JetToken REGULAR_STRING_PART = new JetToken("REGULAR_STRING_PART");
    JetToken ESCAPE_SEQUENCE = new JetToken("ESCAPE_SEQUENCE");
    JetToken SHORT_TEMPLATE_ENTRY_START = new JetToken("SHORT_TEMPLATE_ENTRY_START");
    JetToken LONG_TEMPLATE_ENTRY_START = new JetToken("LONG_TEMPLATE_ENTRY_START");
    JetToken LONG_TEMPLATE_ENTRY_END = new JetToken("LONG_TEMPLATE_ENTRY_END");
    JetToken DANGLING_NEWLINE = new JetToken("DANGLING_NEWLINE");

    JetKeywordToken PACKAGE_KEYWORD = JetKeywordToken.keyword("package");
    JetKeywordToken AS_KEYWORD               = JetKeywordToken.keyword("as");
    JetKeywordToken TYPE_KEYWORD             = JetKeywordToken.keyword("type");
    JetKeywordToken CLASS_KEYWORD            = JetKeywordToken.keyword("class");
    JetKeywordToken THIS_KEYWORD             = JetKeywordToken.keyword("this");
    JetKeywordToken SUPER_KEYWORD             = JetKeywordToken.keyword("super");
    JetKeywordToken VAL_KEYWORD              = JetKeywordToken.keyword("val");
    JetKeywordToken VAR_KEYWORD              = JetKeywordToken.keyword("var");
    JetKeywordToken FUN_KEYWORD              = JetKeywordToken.keyword("fun");
    JetKeywordToken FOR_KEYWORD              = JetKeywordToken.keyword("for");
    JetKeywordToken NULL_KEYWORD             = JetKeywordToken.keyword("null");
    JetKeywordToken TRUE_KEYWORD             = JetKeywordToken.keyword("true");
    JetKeywordToken FALSE_KEYWORD            = JetKeywordToken.keyword("false");
    JetKeywordToken IS_KEYWORD               = JetKeywordToken.keyword("is");
    JetKeywordToken IN_KEYWORD               = JetKeywordToken.keyword("in");
    JetKeywordToken THROW_KEYWORD            = JetKeywordToken.keyword("throw");
    JetKeywordToken RETURN_KEYWORD           = JetKeywordToken.keyword("return");
    JetKeywordToken BREAK_KEYWORD            = JetKeywordToken.keyword("break");
    JetKeywordToken CONTINUE_KEYWORD         = JetKeywordToken.keyword("continue");
    JetKeywordToken OBJECT_KEYWORD           = JetKeywordToken.keyword("object");
    JetKeywordToken IF_KEYWORD               = JetKeywordToken.keyword("if");
    JetKeywordToken TRY_KEYWORD              = JetKeywordToken.keyword("try");
    JetKeywordToken ELSE_KEYWORD             = JetKeywordToken.keyword("else");
    JetKeywordToken WHILE_KEYWORD            = JetKeywordToken.keyword("while");
    JetKeywordToken DO_KEYWORD               = JetKeywordToken.keyword("do");
    JetKeywordToken WHEN_KEYWORD            = JetKeywordToken.keyword("when");
    JetKeywordToken TRAIT_KEYWORD     = JetKeywordToken.keyword("trait");
    // TODO: Discuss "This" keyword
    JetKeywordToken CAPITALIZED_THIS_KEYWORD = JetKeywordToken.keyword("This");


    JetToken AS_SAFE = JetKeywordToken.keyword("AS_SAFE");//new JetToken("as?");

    JetToken IDENTIFIER = new JetToken("IDENTIFIER");
    JetToken LABEL_IDENTIFIER = new JetToken("LABEL_IDENTIFIER");

    JetToken FIELD_IDENTIFIER = new JetToken("FIELD_IDENTIFIER");
    JetToken LBRACKET    = new JetToken("LBRACKET");
    JetToken RBRACKET    = new JetToken("RBRACKET");
    JetToken LBRACE      = new JetToken("LBRACE");
    JetToken RBRACE      = new JetToken("RBRACE");
    JetToken LPAR        = new JetToken("LPAR");
    JetToken RPAR        = new JetToken("RPAR");
    JetToken DOT         = new JetToken("DOT");
    JetToken PLUSPLUS    = new JetToken("PLUSPLUS");
    JetToken MINUSMINUS  = new JetToken("MINUSMINUS");
    JetToken MUL         = new JetToken("MUL");
    JetToken PLUS        = new JetToken("PLUS");
    JetToken MINUS       = new JetToken("MINUS");
    JetToken EXCL        = new JetToken("EXCL");
    JetToken DIV         = new JetToken("DIV");
    JetToken PERC        = new JetToken("PERC");
    JetToken LT          = new JetToken("LT");
    JetToken GT          = new JetToken("GT");
    JetToken LTEQ        = new JetToken("LTEQ");
    JetToken GTEQ        = new JetToken("GTEQ");
    JetToken EQEQEQ      = new JetToken("EQEQEQ");
    JetToken ARROW       = new JetToken("ARROW");
    JetToken DOUBLE_ARROW       = new JetToken("DOUBLE_ARROW");
    JetToken EXCLEQEQEQ  = new JetToken("EXCLEQEQEQ");
    JetToken EQEQ        = new JetToken("EQEQ");
    JetToken EXCLEQ      = new JetToken("EXCLEQ");
    JetToken EXCLEXCL      = new JetToken("EXCLEXCL");
    JetToken ANDAND      = new JetToken("ANDAND");
    JetToken OROR        = new JetToken("OROR");
    JetToken SAFE_ACCESS = new JetToken("SAFE_ACCESS");
    JetToken ELVIS       = new JetToken("ELVIS");
    //    JetToken MAP         = new JetToken("MAP");
//    JetToken FILTER      = new JetToken("FILTER");
    JetToken QUEST       = new JetToken("QUEST");
    JetToken COLONCOLON  = new JetToken("COLONCOLON");
    JetToken COLON       = new JetToken("COLON");
    JetToken SEMICOLON   = new JetToken("SEMICOLON");
    JetToken RANGE       = new JetToken("RANGE");
    JetToken EQ          = new JetToken("EQ");
    JetToken MULTEQ      = new JetToken("MULTEQ");
    JetToken DIVEQ       = new JetToken("DIVEQ");
    JetToken PERCEQ      = new JetToken("PERCEQ");
    JetToken PLUSEQ      = new JetToken("PLUSEQ");
    JetToken MINUSEQ     = new JetToken("MINUSEQ");
    JetToken NOT_IN      = JetKeywordToken.keyword("NOT_IN");
    JetToken NOT_IS      = JetKeywordToken.keyword("NOT_IS");
    JetToken HASH        = new JetToken("HASH");
    JetToken AT          = new JetToken("AT");
    JetToken ATAT          = new JetToken("ATAT");
    
    JetToken IDE_TEMPLATE_START = new JetToken("IDE_TEMPLATE_START");
    JetToken IDE_TEMPLATE_END = new JetToken("IDE_TEMPLATE_END");

    TokenSet LABELS = TokenSet.create(AT, ATAT, LABEL_IDENTIFIER);

    JetToken COMMA       = new JetToken("COMMA");

    JetToken EOL_OR_SEMICOLON   = new JetToken("EOL_OR_SEMICOLON");
    JetKeywordToken IMPORT_KEYWORD    = JetKeywordToken.softKeyword("import");
    JetKeywordToken WHERE_KEYWORD     = JetKeywordToken.softKeyword("where");
    JetKeywordToken BY_KEYWORD        = JetKeywordToken.softKeyword("by");
    JetKeywordToken GET_KEYWORD       = JetKeywordToken.softKeyword("get");
    JetKeywordToken SET_KEYWORD       = JetKeywordToken.softKeyword("set");
    JetKeywordToken ABSTRACT_KEYWORD  = JetKeywordToken.softKeyword("abstract");
    JetKeywordToken ENUM_KEYWORD      = JetKeywordToken.softKeyword("enum");
    JetKeywordToken OPEN_KEYWORD      = JetKeywordToken.softKeyword("open");
    JetKeywordToken INNER_KEYWORD     = JetKeywordToken.softKeyword("inner");
    JetKeywordToken ANNOTATION_KEYWORD = JetKeywordToken.softKeyword("annotation");
    JetKeywordToken OVERRIDE_KEYWORD  = JetKeywordToken.softKeyword("override");
    JetKeywordToken PRIVATE_KEYWORD   = JetKeywordToken.softKeyword("private");
    JetKeywordToken PUBLIC_KEYWORD    = JetKeywordToken.softKeyword("public");
    JetKeywordToken INTERNAL_KEYWORD  = JetKeywordToken.softKeyword("internal");
    JetKeywordToken PROTECTED_KEYWORD = JetKeywordToken.softKeyword("protected");
    JetKeywordToken CATCH_KEYWORD     = JetKeywordToken.softKeyword("catch");
    JetKeywordToken OUT_KEYWORD       = JetKeywordToken.softKeyword("out");
    JetKeywordToken VARARG_KEYWORD       = JetKeywordToken.softKeyword("vararg");
    JetKeywordToken INLINE_KEYWORD       = JetKeywordToken.softKeyword("inline");
    JetKeywordToken REIFIED_KEYWORD = JetKeywordToken.softKeyword("reified");

    JetKeywordToken FINALLY_KEYWORD   = JetKeywordToken.softKeyword("finally");
    JetKeywordToken FINAL_KEYWORD   = JetKeywordToken.softKeyword("final");

    TokenSet KEYWORDS = TokenSet.create(PACKAGE_KEYWORD, AS_KEYWORD, TYPE_KEYWORD, CLASS_KEYWORD, TRAIT_KEYWORD,
            THIS_KEYWORD, SUPER_KEYWORD, VAL_KEYWORD, VAR_KEYWORD, FUN_KEYWORD, FOR_KEYWORD,
            NULL_KEYWORD,
            TRUE_KEYWORD, FALSE_KEYWORD, IS_KEYWORD,
            IN_KEYWORD, THROW_KEYWORD, RETURN_KEYWORD, BREAK_KEYWORD, CONTINUE_KEYWORD, OBJECT_KEYWORD, IF_KEYWORD,
            ELSE_KEYWORD, WHILE_KEYWORD, DO_KEYWORD, TRY_KEYWORD, WHEN_KEYWORD,
            NOT_IN, NOT_IS, CAPITALIZED_THIS_KEYWORD, AS_SAFE
    );

    TokenSet SOFT_KEYWORDS = TokenSet.create(IMPORT_KEYWORD, WHERE_KEYWORD, BY_KEYWORD, GET_KEYWORD,
            SET_KEYWORD, ABSTRACT_KEYWORD, ENUM_KEYWORD, OPEN_KEYWORD, INNER_KEYWORD, ANNOTATION_KEYWORD,
            OVERRIDE_KEYWORD, PRIVATE_KEYWORD, PUBLIC_KEYWORD, INTERNAL_KEYWORD, PROTECTED_KEYWORD,
            CATCH_KEYWORD, FINALLY_KEYWORD, OUT_KEYWORD, FINAL_KEYWORD, VARARG_KEYWORD, INLINE_KEYWORD, REIFIED_KEYWORD
    );

    TokenSet MODIFIER_KEYWORDS = TokenSet.create(ABSTRACT_KEYWORD, ENUM_KEYWORD,
            OPEN_KEYWORD, INNER_KEYWORD, ANNOTATION_KEYWORD, OVERRIDE_KEYWORD, PRIVATE_KEYWORD, PUBLIC_KEYWORD, INTERNAL_KEYWORD,
            PROTECTED_KEYWORD, OUT_KEYWORD, IN_KEYWORD, FINAL_KEYWORD, VARARG_KEYWORD, INLINE_KEYWORD, REIFIED_KEYWORD
    );
    TokenSet WHITE_SPACE_OR_COMMENT_BIT_SET = TokenSet.create(WHITE_SPACE, BLOCK_COMMENT, EOL_COMMENT, DOC_COMMENT, SHEBANG_COMMENT);
    TokenSet WHITESPACES = TokenSet.create(TokenType.WHITE_SPACE);
    TokenSet COMMENTS = TokenSet.create(EOL_COMMENT, BLOCK_COMMENT, DOC_COMMENT, SHEBANG_COMMENT);

    TokenSet STRINGS = TokenSet.create(CHARACTER_LITERAL, REGULAR_STRING_PART);
    TokenSet OPERATIONS = TokenSet.create(AS_KEYWORD, AS_SAFE, IS_KEYWORD, IN_KEYWORD, DOT, PLUSPLUS, MINUSMINUS, EXCLEXCL, MUL, PLUS,
            MINUS, EXCL, DIV, PERC, LT, GT, LTEQ, GTEQ, EQEQEQ, EXCLEQEQEQ, EQEQ, EXCLEQ, ANDAND, OROR,
            SAFE_ACCESS, ELVIS,
//            MAP, FILTER,
            COLON,
            RANGE, EQ, MULTEQ, DIVEQ, PERCEQ, PLUSEQ, MINUSEQ,
            NOT_IN, NOT_IS,
            IDENTIFIER, LABEL_IDENTIFIER, ATAT, AT);

    TokenSet AUGMENTED_ASSIGNMENTS = TokenSet.create(PLUSEQ, MINUSEQ, MULTEQ, PERCEQ, DIVEQ);
}
