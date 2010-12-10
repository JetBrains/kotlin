/*
 * @author max
 */
package org.jetbrains.jet.lexer;

import com.intellij.psi.TokenType;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.TokenSet;
import gnu.trove.THashSet;

import java.util.Arrays;
import java.util.Set;

public interface JetTokens {
    JetToken BLOCK_COMMENT = new JetToken("BLOCK_COMMENT");
    JetToken DOC_COMMENT = new JetToken("DOC_COMMENT");
    JetToken EOL_COMMENT   = new JetToken("EOL_COMMENT");

    IElementType WHITE_SPACE = TokenType.WHITE_SPACE;

    JetToken INTEGER_LITERAL    = new JetToken("INTEGER_LITERAL");
    JetToken FLOAT_LITERAL      = new JetToken("FLOAT_LITERAL");
    JetToken CHARACTER_LITERAL  = new JetToken("CHARACTER_LITERAL");
    JetToken STRING_LITERAL     = new JetToken("STRING_LITERAL");
    JetToken RAW_STRING_LITERAL = new JetToken("RAW_STRING_LITERAL");

    JetToken NAMESPACE_KEYWORD  = new JetToken("NAMESPACE_KEYWORD");
    JetToken IMPORT_KEYWORD     = new JetToken("IMPORT_KEYWORD");
    JetToken AS_KEYWORD         = new JetToken("AS_KEYWORD");
    JetToken TYPE_KEYWORD       = new JetToken("TYPE_KEYWORD");
    JetToken CLASS_KEYWORD      = new JetToken("CLASS_KEYWORD");
    JetToken THIS_KEYWORD       = new JetToken("THIS_KEYWORD");
    JetToken VAL_KEYWORD        = new JetToken("VAL_KEYWORD");
    JetToken VAR_KEYWORD        = new JetToken("VAR_KEYWORD");
    JetToken FUN_KEYWORD        = new JetToken("FUN_KEYWORD");
    JetToken DECOMPOSER_KEYWORD = new JetToken("DECOMPOSER_KEYWORD");
    JetToken EXTENSION_KEYWORD  = new JetToken("EXTENSION_KEYWORD");
    JetToken FOR_KEYWORD        = new JetToken("FOR_KEYWORD");
    JetToken NULL_KEYWORD       = new JetToken("NULL_KEYWORD");
    JetToken TYPEOF_KEYWORD     = new JetToken("TYPEOF_KEYWORD");
    JetToken NEW_KEYWORD        = new JetToken("NEW_KEYWORD");
    JetToken TRUE_KEYWORD       = new JetToken("TRUE_KEYWORD");
    JetToken FALSE_KEYWORD      = new JetToken("FALSE_KEYWORD");
    JetToken IS_KEYWORD         = new JetToken("IS_KEYWORD");
    JetToken ISNOT_KEYWORD      = new JetToken("ISNOT_KEYWORD");
    JetToken IN_KEYWORD         = new JetToken("IN_KEYWORD");
    JetToken THROW_KEYWORD      = new JetToken("THROW_KEYWORD");
    JetToken RETURN_KEYWORD     = new JetToken("RETURN_KEYWORD");
    JetToken BREAK_KEYWORD      = new JetToken("BREAK_KEYWORD");
    JetToken CONTINUE_KEYWORD   = new JetToken("CONTINUE_KEYWORD");
    JetToken OBJECT_KEYWORD     = new JetToken("OBJECT_KEYWORD");
    JetToken IF_KEYWORD         = new JetToken("IF_KEYWORD");
    JetToken ELSE_KEYWORD       = new JetToken("ELSE_KEYWORD");
    JetToken WHILE_KEYWORD      = new JetToken("WHILE_KEYWORD");
    JetToken DO_KEYWORD         = new JetToken("DO_KEYWORD");
    JetToken MATCH_KEYWORD      = new JetToken("MATCH_KEYWORD");


    JetToken IDENTIFIER = new JetToken("IDENTIFIER");

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
    JetToken EQEQ        = new JetToken("EQEQ");
    JetToken EXCLEQ      = new JetToken("EXCLEQ");
    JetToken ANDAND      = new JetToken("ANDAND");
    JetToken OROR        = new JetToken("OROR");
    JetToken SAFE_ACCESS = new JetToken("SAFE_ACCESS");
    JetToken ELVIS       = new JetToken("ELVIS");
    JetToken MAP         = new JetToken("MAP");
    JetToken FILTER      = new JetToken("FILTER");
    JetToken QUEST       = new JetToken("QUEST");
    JetToken COLON       = new JetToken("COLON");
    JetToken SEMICOLON   = new JetToken("SEMICOLON");
    JetToken RANGE       = new JetToken("RANGE");
    JetToken EQ          = new JetToken("EQ");
    JetToken MULTEQ      = new JetToken("MULTEQ");
    JetToken DIVEQ       = new JetToken("DIVEQ");
    JetToken PERCEQ      = new JetToken("PERCEQ");
    JetToken PLUSEQ      = new JetToken("PLUSEQ");
    JetToken MINUSEQ     = new JetToken("MINUSEQ");
    JetToken COMMA       = new JetToken("COMMA");

    JetToken WRAPS_KEYWORD     = new JetToken("wraps");
    JetToken WHERE_KEYWORD     = new JetToken("where");
    JetToken BY_KEYWORD        = new JetToken("by");
    JetToken LAZY_KEYWORD      = new JetToken("lazy");
    JetToken GET_KEYWORD       = new JetToken("get");
    JetToken SET_KEYWORD       = new JetToken("set");
    JetToken ABSTRACT_KEYWORD  = new JetToken("abstract");
    JetToken VIRTUAL_KEYWORD   = new JetToken("virtual");
    JetToken ENUM_KEYWORD      = new JetToken("enum");
    JetToken OPEN_KEYWORD      = new JetToken("open");
    JetToken ATTRIBUTE_KEYWORD = new JetToken("attribute");
    JetToken OVERRIDE_KEYWORD  = new JetToken("override");
    JetToken PRIVATE_KEYWORD   = new JetToken("private");
    JetToken PUBLIC_KEYWORD    = new JetToken("public");
    JetToken INTERNAL_KEYWORD  = new JetToken("internal");
    JetToken PROTECTED_KEYWORD = new JetToken("protected");
    JetToken OUT_KEYWORD       = new JetToken("out");
    JetToken REF_KEYWORD       = new JetToken("ref");

    TokenSet KEYWORDS = TokenSet.create(NAMESPACE_KEYWORD, IMPORT_KEYWORD, AS_KEYWORD, TYPE_KEYWORD, CLASS_KEYWORD,
            THIS_KEYWORD, VAL_KEYWORD, VAR_KEYWORD, FUN_KEYWORD, DECOMPOSER_KEYWORD, EXTENSION_KEYWORD, FOR_KEYWORD,
            NULL_KEYWORD, TYPEOF_KEYWORD, NEW_KEYWORD, TRUE_KEYWORD, FALSE_KEYWORD, IS_KEYWORD, ISNOT_KEYWORD,
            IN_KEYWORD, THROW_KEYWORD, RETURN_KEYWORD, BREAK_KEYWORD, CONTINUE_KEYWORD, OBJECT_KEYWORD, IF_KEYWORD,
            ELSE_KEYWORD, WHILE_KEYWORD, DO_KEYWORD, MATCH_KEYWORD
    );

    TokenSet SOFT_KEYWORDS = TokenSet.create(WRAPS_KEYWORD, WHERE_KEYWORD, BY_KEYWORD, LAZY_KEYWORD, GET_KEYWORD,
            SET_KEYWORD, ABSTRACT_KEYWORD, VIRTUAL_KEYWORD, ENUM_KEYWORD, OPEN_KEYWORD, ATTRIBUTE_KEYWORD,
            OVERRIDE_KEYWORD, PRIVATE_KEYWORD, PUBLIC_KEYWORD, INTERNAL_KEYWORD, PROTECTED_KEYWORD, OUT_KEYWORD,
            REF_KEYWORD
    );
}
