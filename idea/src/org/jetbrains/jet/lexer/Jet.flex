/* It's an automatically generated code. Do not modify it. */
package org.jetbrains.jet.lexer;

import com.intellij.lexer.*;
import com.intellij.psi.*;
import com.intellij.psi.tree.IElementType;

import org.jetbrains.jet.lexer.JetTokens;

%%

%unicode
%class _JetLexer
%implements FlexLexer
%function advance
%type IElementType
%eof{  return;
%eof}

DIGIT=[0-9]
HEX_DIGIT=[0-9A-Fa-f]
WHITE_SPACE_CHAR=[\ \n\t\f]

// TODO: prohibit '$' in identifiers?
PLAIN_IDENTIFIER=[:jletter:] [:jletterdigit:]*
// TODO: this one MUST allow everything accepted by the runtime
// TODO: Replace backticks by one backslash in the begining
ESCAPED_IDENTIFIER = `{PLAIN_IDENTIFIER}`
IDENTIFIER = {PLAIN_IDENTIFIER}|{ESCAPED_IDENTIFIER}
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
ESCAPE_SEQUENCE=\\[^\n]

// ANY_ESCAPE_SEQUENCE = \\[^]
THREE_QUO = (\"\"\")
ONE_TWO_QUO = (\"[^\"]) | (\"\"[^\"])
QUO_STRING_CHAR = [^\"] | {ONE_TWO_QUO}
RAW_STRING_LITERAL = {THREE_QUO} {QUO_STRING_CHAR}* {THREE_QUO}?

%%

<YYINITIAL> {BLOCK_COMMENT} { return JetTokens.BLOCK_COMMENT; }
<YYINITIAL> {DOC_COMMENT} { return JetTokens.DOC_COMMENT; }

<YYINITIAL> ({WHITE_SPACE_CHAR})+ { return JetTokens.WHITE_SPACE; }

<YYINITIAL> {EOL_COMMENT} { return JetTokens.EOL_COMMENT; }

<YYINITIAL> {INTEGER_LITERAL}\.\. { yypushback(2); return JetTokens.INTEGER_LITERAL; }
<YYINITIAL> {INTEGER_LITERAL} { return JetTokens.INTEGER_LITERAL; }
//<YYINITIAL> {LONG_LITERAL} { return JetTokens.LONG_LITERAL; }

//<YYINITIAL> {FLOAT_LITERAL}      { return JetTokens.FLOAT_LITERAL; }
//<YYINITIAL> {HEX_FLOAT_LITERAL}  { return JetTokens.FLOAT_LITERAL; }
<YYINITIAL> {DOUBLE_LITERAL}     { return JetTokens.FLOAT_LITERAL; }
<YYINITIAL> {HEX_DOUBLE_LITERAL} { return JetTokens.FLOAT_LITERAL; }

<YYINITIAL> {CHARACTER_LITERAL} { return JetTokens.CHARACTER_LITERAL; }
<YYINITIAL> {STRING_LITERAL} { return JetTokens.STRING_LITERAL; }
// TODO: Decide what to do with """ ... """"
<YYINITIAL> {RAW_STRING_LITERAL} { return JetTokens.RAW_STRING_LITERAL; }

<YYINITIAL> "namespace"  { return JetTokens.NAMESPACE_KEYWORD ;}
<YYINITIAL> "extension"  { return JetTokens.EXTENSION_KEYWORD ;}
<YYINITIAL> "continue"   { return JetTokens.CONTINUE_KEYWORD ;}
<YYINITIAL> "return"     { return JetTokens.RETURN_KEYWORD ;}
<YYINITIAL> "typeof"     { return JetTokens.TYPEOF_KEYWORD ;}
<YYINITIAL> "object"     { return JetTokens.OBJECT_KEYWORD ;}
<YYINITIAL> "while"      { return JetTokens.WHILE_KEYWORD ;}
<YYINITIAL> "break"      { return JetTokens.BREAK_KEYWORD ;}
<YYINITIAL> "class"      { return JetTokens.CLASS_KEYWORD ;}
<YYINITIAL> "throw"      { return JetTokens.THROW_KEYWORD ;}
<YYINITIAL> "false"      { return JetTokens.FALSE_KEYWORD ;}
<YYINITIAL> "when"       { return JetTokens.WHEN_KEYWORD ;}
<YYINITIAL> "true"       { return JetTokens.TRUE_KEYWORD ;}
<YYINITIAL> "type"       { return JetTokens.TYPE_KEYWORD ;}
<YYINITIAL> "this"       { return JetTokens.THIS_KEYWORD ;}
<YYINITIAL> "null"       { return JetTokens.NULL_KEYWORD ;}
<YYINITIAL> "else"       { return JetTokens.ELSE_KEYWORD ;}
<YYINITIAL> "This"       { return JetTokens.CAPITALIZED_THIS_KEYWORD ;}
<YYINITIAL> "try"        { return JetTokens.TRY_KEYWORD ;}
<YYINITIAL> "val"        { return JetTokens.VAL_KEYWORD ;}
<YYINITIAL> "var"        { return JetTokens.VAR_KEYWORD ;}
<YYINITIAL> "fun"        { return JetTokens.FUN_KEYWORD ;}
<YYINITIAL> "for"        { return JetTokens.FOR_KEYWORD ;}
//<YYINITIAL> "new"        { return JetTokens.NEW_KEYWORD ;}
<YYINITIAL> "is"         { return JetTokens.IS_KEYWORD ;}
<YYINITIAL> "in"         { return JetTokens.IN_KEYWORD ;}
<YYINITIAL> "if"         { return JetTokens.IF_KEYWORD ;}
<YYINITIAL> "do"         { return JetTokens.DO_KEYWORD ;}
<YYINITIAL> "as"         { return JetTokens.AS_KEYWORD ;}

<YYINITIAL> {FIELD_IDENTIFIER} { return JetTokens.FIELD_IDENTIFIER; }
<YYINITIAL> {IDENTIFIER} { return JetTokens.IDENTIFIER; }
<YYINITIAL> {LABEL_IDENTIFIER}   { return JetTokens.LABEL_IDENTIFIER; }

<YYINITIAL> "==="        { return JetTokens.EQEQEQ    ; }
<YYINITIAL> "!=="        { return JetTokens.EXCLEQEQEQ; }
<YYINITIAL> "!in"        { return JetTokens.NOT_IN; }
<YYINITIAL> "!is"        { return JetTokens.NOT_IS; }
<YYINITIAL> "as?"        { return JetTokens.AS_SAFE; }
<YYINITIAL> "++"         { return JetTokens.PLUSPLUS  ; }
<YYINITIAL> "--"         { return JetTokens.MINUSMINUS; }
<YYINITIAL> "<="         { return JetTokens.LTEQ      ; }
<YYINITIAL> ">="         { return JetTokens.GTEQ      ; }
<YYINITIAL> "=="         { return JetTokens.EQEQ      ; }
<YYINITIAL> "!="         { return JetTokens.EXCLEQ    ; }
<YYINITIAL> "&&"         { return JetTokens.ANDAND    ; }
<YYINITIAL> "||"         { return JetTokens.OROR      ; }
<YYINITIAL> "?."         { return JetTokens.SAFE_ACCESS;}
<YYINITIAL> "?:"         { return JetTokens.ELVIS     ; }
//<YYINITIAL> ".*"         { return JetTokens.MAP       ; }
//<YYINITIAL> ".?"         { return JetTokens.FILTER    ; }
<YYINITIAL> "*="         { return JetTokens.MULTEQ    ; }
<YYINITIAL> "/="         { return JetTokens.DIVEQ     ; }
<YYINITIAL> "%="         { return JetTokens.PERCEQ    ; }
<YYINITIAL> "+="         { return JetTokens.PLUSEQ    ; }
<YYINITIAL> "-="         { return JetTokens.MINUSEQ   ; }
<YYINITIAL> "->"         { return JetTokens.ARROW     ; }
<YYINITIAL> "=>"         { return JetTokens.DOUBLE_ARROW; }
<YYINITIAL> ".."         { return JetTokens.RANGE     ; }
<YYINITIAL> "@@"         { return JetTokens.ATAT      ; }
<YYINITIAL> "["          { return JetTokens.LBRACKET  ; }
<YYINITIAL> "]"          { return JetTokens.RBRACKET  ; }
<YYINITIAL> "{"          { return JetTokens.LBRACE    ; }
<YYINITIAL> "}"          { return JetTokens.RBRACE    ; }
<YYINITIAL> "("          { return JetTokens.LPAR      ; }
<YYINITIAL> ")"          { return JetTokens.RPAR      ; }
<YYINITIAL> "."          { return JetTokens.DOT       ; }
<YYINITIAL> "*"          { return JetTokens.MUL       ; }
<YYINITIAL> "+"          { return JetTokens.PLUS      ; }
<YYINITIAL> "-"          { return JetTokens.MINUS     ; }
<YYINITIAL> "!"          { return JetTokens.EXCL      ; }
<YYINITIAL> "/"          { return JetTokens.DIV       ; }
<YYINITIAL> "%"          { return JetTokens.PERC      ; }
<YYINITIAL> "<"          { return JetTokens.LT        ; }
<YYINITIAL> ">"          { return JetTokens.GT        ; }
<YYINITIAL> "?"          { return JetTokens.QUEST     ; }
<YYINITIAL> ":"          { return JetTokens.COLON     ; }
<YYINITIAL> ";"          { return JetTokens.SEMICOLON ; }
<YYINITIAL> "="          { return JetTokens.EQ        ; }
<YYINITIAL> ","          { return JetTokens.COMMA     ; }
<YYINITIAL> "#"          { return JetTokens.HASH      ; }
<YYINITIAL> "@"          { return JetTokens.AT        ; }

<YYINITIAL> . { return TokenType.BAD_CHARACTER; }

