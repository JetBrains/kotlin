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

  private final void pushbackEnd() {
    if (isLastToken() && CharArrayUtil.regionMatches(zzBuffer, zzMarkedPos - 2, zzMarkedPos, "*/")) {
      yypushback(2);
    }
  }

  private final void pushbackText() {
    int i = zzStartRead;
    while (zzBuffer.charAt(i) == '*') i++;
    yypushback(zzMarkedPos - i);
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
"*"+ "/"                                  { if (isLastToken())
                                              return KDocTokens.END;            }

// hack: make longest match
<LINE_BEGINNING> "*"+ {NOT_WHITE_SPACE_CHAR}* { pushbackText();
                                                yybegin(CONTENTS);
                                                return KDocTokens.LEADING_ASTERISK; }

<CONTENTS, LINE_BEGINNING> {
            // todo: put markdown and @tags token patterns here

            {NOT_WHITE_SPACE_CHAR}+        { pushbackEnd();
                                             return KDocTokens.TEXT;            }
            {WHITE_SPACE_CHAR}+            { if (yytextContainLineBreaks())
                                               yybegin(LINE_BEGINNING);
                                             return TokenType.WHITE_SPACE;      }
}

.                                         { return TokenType.BAD_CHARACTER;     }