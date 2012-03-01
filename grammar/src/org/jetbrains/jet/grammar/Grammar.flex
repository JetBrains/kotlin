/* It's an automatically generated code. Do not modify it. */
package org.jetbrains.jet.grammar;

//import com.intellij.lexer.*;
//import com.intellij.psi.*;
//import com.intellij.psi.tree.IElementType;

//import org.jetbrains.jet.lexer.JetTokens;

%%

%unicode
%class _GrammarLexer
%{
    private String heredoc;
%}

%function advance
%type Token
%eof{  return;
%eof}
%state HEREDOC

DIGIT=[0-9]
HEX_DIGIT=[0-9A-Fa-f]
WHITE_SPACE_CHAR=[\ \n\t\f]

// TODO: prohibit '$' in identifiers?
PLAIN_IDENTIFIER=[:jletter:] [:jletterdigit:]*
// TODO: this one MUST allow everything accepted by the runtime
// TODO: Replace backticks by one backslash in the begining
ESCAPED_IDENTIFIER = `{PLAIN_IDENTIFIER}`
IDENTIFIER = {PLAIN_IDENTIFIER}|{ESCAPED_IDENTIFIER}
DECLARATION_IDENTIFIER = "&" {PLAIN_IDENTIFIER}
FIELD_IDENTIFIER = \${IDENTIFIER}
LABEL_IDENTIFIER = \@{IDENTIFIER}

BLOCK_COMMENT=("/*"[^"*"]{COMMENT_TAIL})|"/*"
// TODO: Wiki markup for doc comments?
DOC_COMMENT="/*""*"+("/"|([^"/""*"]{COMMENT_TAIL}))?
COMMENT_TAIL=([^"*"]*("*"+[^"*""/"])?)*("*"+"/")?
EOL_COMMENT="/""/"[^\n]*

INTEGER_LITERAL={DECIMAL_INTEGER_LITERAL}|{HEX_INTEGER_LITERAL}|{BIN_INTEGER_LITERAL}
DECIMAL_INTEGER_LITERAL=(0|([1-9]({DIGIT})*))
HEX_INTEGER_LITERAL=0[Xx]({HEX_DIGIT})*
BIN_INTEGER_LITERAL=0[Bb]({DIGIT})*
LONG_LITERAL=({INTEGER_LITERAL})[Ll]

//FLOAT_LITERAL=(({FLOATING_POINT_LITERAL1})[Ff])|(({FLOATING_POINT_LITERAL2})[Ff])|(({FLOATING_POINT_LITERAL3})[Ff])|(({FLOATING_POINT_LITERAL4})[Ff])
//DOUBLE_LITERAL=(({FLOATING_POINT_LITERAL1})[Dd]?)|(({FLOATING_POINT_LITERAL2})[Dd]?)|(({FLOATING_POINT_LITERAL3})[Dd]?)|(({FLOATING_POINT_LITERAL4})[Dd])
DOUBLE_LITERAL={FLOATING_POINT_LITERAL1}|{FLOATING_POINT_LITERAL2}|{FLOATING_POINT_LITERAL3}|{FLOATING_POINT_LITERAL4}
FLOATING_POINT_LITERAL1=({DIGIT})+"."({DIGIT})+({EXPONENT_PART})?
FLOATING_POINT_LITERAL2="."({DIGIT})+({EXPONENT_PART})?
FLOATING_POINT_LITERAL3=({DIGIT})+({EXPONENT_PART})
FLOATING_POINT_LITERAL4=({DIGIT})+
EXPONENT_PART=[Ee]["+""-"]?({DIGIT})*
HEX_FLOAT_LITERAL={HEX_SIGNIFICAND}{BINARY_EXPONENT}[Ff]
//HEX_DOUBLE_LITERAL={HEX_SIGNIFICAND}{BINARY_EXPONENT}[Dd]?
HEX_DOUBLE_LITERAL={HEX_SIGNIFICAND}{BINARY_EXPONENT}?
BINARY_EXPONENT=[Pp][+-]?{DIGIT}+
HEX_SIGNIFICAND={HEX_INTEGER_LITERAL}|0[Xx]{HEX_DIGIT}*\.{HEX_DIGIT}+
//HEX_SIGNIFICAND={HEX_INTEGER_LITERAL}|{HEX_INTEGER_LITERAL}\.|0[Xx]{HEX_DIGIT}*\.{HEX_DIGIT}+

CHARACTER_LITERAL="'"([^\\\'\n]|{ESCAPE_SEQUENCE})*("'"|\\)?
// TODO: introduce symbols (e.g. 'foo) as another way to write string literals
STRING_LITERAL=\"([^\\\"\n]|{ESCAPE_SEQUENCE})*(\"|\\)?
ANGLE_STRING_LITERAL=\<([^\\\>\n])*(\>|\\)?
ESCAPE_SEQUENCE=\\[^\n]

// ANY_ESCAPE_SEQUENCE = \\[^]
THREE_QUO = (\"\"\")
ONE_TWO_QUO = (\"[^\"]) | (\"\"[^\"])
QUO_STRING_CHAR = [^\"] | {ONE_TWO_QUO}
RAW_STRING_LITERAL = {THREE_QUO} {QUO_STRING_CHAR}* {THREE_QUO}?
%%

// HEREDOCS

<YYINITIAL>(\<\<\<){IDENTIFIER}\n {
                                this.heredoc = yytext().toString().substring(3).trim();
                                yybegin(HEREDOC);
                            }

<HEREDOC>[^\n]*\n           {
                                String text = yytext().toString().trim();
                                if (this.heredoc.equals(text)) {
                                    yybegin(YYINITIAL);
                                    yypushback(1);
                                    return new StringToken(yytext());
                                }
                            }



<YYINITIAL> {BLOCK_COMMENT} { return new Comment(yytext()); }
<YYINITIAL> {DOC_COMMENT} { return new DocComment(yytext()); }

<YYINITIAL> ({WHITE_SPACE_CHAR})+ { return new WhiteSpace(yytext()); }

<YYINITIAL> {EOL_COMMENT} { return new Comment(yytext()); }

<YYINITIAL> {STRING_LITERAL} { return new StringToken(yytext()); }
<YYINITIAL> {ANGLE_STRING_LITERAL} { return new StringToken(yytext()); }
<YYINITIAL> {IDENTIFIER} { return new Identifier(yytext()); }
<YYINITIAL> "[" {IDENTIFIER} "]" { return new Annotation(yytext()); }
<YYINITIAL> {DECLARATION_IDENTIFIER} { return new Declaration(yytext()); }

<YYINITIAL> ":"          { return new SymbolToken(yytext()); }
<YYINITIAL> "{"          { return new SymbolToken("\\" + yytext()); }
<YYINITIAL> "}"          { return new SymbolToken("\\" + yytext()); }
<YYINITIAL> "["          { return new SymbolToken("\\" + yytext()); }
<YYINITIAL> "]"          { return new SymbolToken("\\" + yytext()); }
<YYINITIAL> "("          { return new SymbolToken("\\" + yytext()); }
<YYINITIAL> ")"          { return new SymbolToken("\\" + yytext()); }
<YYINITIAL> "*"          { return new SymbolToken(yytext()); }
<YYINITIAL> "+"          { return new SymbolToken(yytext()); }
<YYINITIAL> "?"          { return new SymbolToken(yytext()); }
<YYINITIAL> "|"          { return new SymbolToken(yytext()); }
<YYINITIAL> "-"          { return new SymbolToken(yytext()); }
<YYINITIAL> "."          { return new SymbolToken(yytext()); }

<YYINITIAL> . { return new Other(yytext()); }

