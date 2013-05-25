package org.jetbrains.jet.kdoc.lexer;

import com.intellij.lexer.FlexLexer;
import com.intellij.psi.TokenType;
import com.intellij.psi.tree.IElementType;
import com.intellij.util.text.CharArrayUtil;

%%

%unicode
%class _KDocLexer
%implements FlexLexer

%{
  public _KDocLexer() {
    this((java.io.Reader)null);
  }

  private final boolean isLastToken() {
    return zzMarkedPos == zzBuffer.length();
  }

  private final Boolean yytextContainLineBreaks() {
    return CharArrayUtil.containLineBreaks(zzBuffer, zzStartRead, zzMarkedPos);
  }
%}

%function advance
%type IElementType
%eof{
  return;
%eof}

%state CONTENTS
%state LINE_BEGINNING

WHITE_SPACE_CHAR    =[\ \t\f\n\r]
NOT_WHITE_SPACE_CHAR=[^\ \t\f\n\r]

%%


<YYINITIAL> "/**"                         { yybegin(CONTENTS);
                                            return KDocTokens.START;            }
"*"+ "/"                                  { if (isLastToken()) return KDocTokens.END;
                                            else return KDocTokens.TEXT; }

<LINE_BEGINNING> "*"+                     { yybegin(CONTENTS);
                                            return KDocTokens.LEADING_ASTERISK; }

<CONTENTS, LINE_BEGINNING> {
    {WHITE_SPACE_CHAR}+ {
        if (yytextContainLineBreaks()) {
            yybegin(LINE_BEGINNING);
            return TokenType.WHITE_SPACE;
        } else {
            yybegin(CONTENTS);
            return KDocTokens.TEXT;  // internal white space
        }
    }

  . { yybegin(CONTENTS);
      return KDocTokens.TEXT; }
}

. { return TokenType.BAD_CHARACTER; }