package org.jetbrains.kotlin.kdoc.lexer;

import com.intellij.lexer.FlexLexer;
import com.intellij.psi.TokenType;
import com.intellij.psi.tree.IElementType;
import com.intellij.util.text.CharArrayUtil;
import java.lang.Character;

%%

%unicode
%class _KDocLexer
%implements FlexLexer

%{
  public _KDocLexer() {
    this((java.io.Reader)null);
  }

  private boolean isLastToken() {
    return zzMarkedPos == zzBuffer.length();
  }

  private Boolean yytextContainLineBreaks() {
    return CharArrayUtil.containLineBreaks(zzBuffer, zzStartRead, zzMarkedPos);
  }

  private boolean nextIsNotWhitespace() {
    return zzMarkedPos <= zzBuffer.length() && !Character.isWhitespace(zzBuffer.charAt(zzMarkedPos + 1));
  }

  private boolean prevIsNotWhitespace() {
    return zzMarkedPos != 0 && !Character.isWhitespace(zzBuffer.charAt(zzMarkedPos - 1));
  }
%}

%function advance
%type IElementType
%eof{
  return;
%eof}

%state LINE_BEGINNING
%state CONTENTS_BEGINNING
%state TAG_BEGINNING
%state CONTENTS

WHITE_SPACE_CHAR    =[\ \t\f\n\r]
NOT_WHITE_SPACE_CHAR=[^\ \t\f\n\r]

DIGIT=[0-9]
ALPHA=[:jletter:]
TAG_NAME={ALPHA}({ALPHA}|{DIGIT})*
IDENTIFIER={ALPHA}({ALPHA}|{DIGIT}|".")*
CODE_LINK_START={ALPHA}
CODE_LINK_CHAR={ALPHA}|{DIGIT}|[()\-\.<>]
CODE_LINK=\[{CODE_LINK_START}{CODE_LINK_CHAR}*\]

%%


<YYINITIAL> "/**"                         { yybegin(CONTENTS);
                                            return KDocTokens.START;            }
"*"+ "/"                                  { if (isLastToken()) return KDocTokens.END;
                                            else return KDocTokens.TEXT; }

<LINE_BEGINNING> "*"+                     { yybegin(CONTENTS_BEGINNING);
                                            return KDocTokens.LEADING_ASTERISK; }

<CONTENTS_BEGINNING> "@"{TAG_NAME}          { yybegin(TAG_BEGINNING);
                                              return KDocTokens.TAG_NAME; }

<TAG_BEGINNING> {
    {WHITE_SPACE_CHAR}+ {
        if (yytextContainLineBreaks()) {
            yybegin(LINE_BEGINNING);
        }
        return TokenType.WHITE_SPACE;
    }

    /* Example: @return[x] The return value of function x
                       ^^^
    */
    {CODE_LINK} { yybegin(CONTENTS);
                  return KDocTokens.MARKDOWN_LINK; }

    /* Example: @param aaa The value of aaa
                       ^^^
    */
    {IDENTIFIER} {
        yybegin(CONTENTS);
        return KDocTokens.TEXT_OR_LINK;
    }

    . {
        yybegin(CONTENTS);
        return KDocTokens.TEXT;
    }
}

<LINE_BEGINNING, CONTENTS_BEGINNING, CONTENTS> {
    {WHITE_SPACE_CHAR}+ {
        if (yytextContainLineBreaks()) {
            yybegin(LINE_BEGINNING);
            return TokenType.WHITE_SPACE;
        } else {
            yybegin(yystate() == CONTENTS_BEGINNING? CONTENTS_BEGINNING:CONTENTS);
            return KDocTokens.TEXT;  // internal white space
        }
    }

    "\\"[\[\]]   { yybegin(CONTENTS);
                   return KDocTokens.MARKDOWN_ESCAPED_CHAR; }

    /* We're only interested in parsing links that can become code references,
       meaning they contain only identifier characters and characters that can be
       used in type declarations. No brackets, backticks, asterisks or anything like that. */
    {CODE_LINK} { yybegin(CONTENTS);
                  return KDocTokens.MARKDOWN_LINK; }

    .     { yybegin(CONTENTS);
            return KDocTokens.TEXT; }
}

. { return TokenType.BAD_CHARACTER; }
