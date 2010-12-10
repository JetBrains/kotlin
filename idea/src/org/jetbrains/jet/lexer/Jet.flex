/* It's an automatically generated code. Do not modify it. */
package org.jetbrains.jet.lexer;

import com.intellij.lexer.*;
import com.intellij.psi.*;
import com.intellij.psi.tree.IElementType;


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
BINARY_DIGIT=[0-1]

IDENTIFIER=[:jletter:] [:jletterdigit:]*

EOL_ESC=\\[\ \t]*\n
NON_EOL=[^\n]|{EOL_ESC}

BLOCK_COMMENT=("/*" {COMMENT_TAIL})|"/*"
COMMENT_TAIL=([^"*"]*("*"+[^"*""/"])?)*("*"+"/")?
EOL_COMMENT="/""/"{NON_EOL}*

INTEGER_LITERAL=({DECIMAL_INTEGER_LITERAL}|{HEX_INTEGER_LITERAL}|{BINARY_INTEGER_LITERAL})
DECIMAL_INTEGER_LITERAL=(0|([1-9]({DIGIT})*))
HEX_INTEGER_LITERAL=0[Xx]({HEX_DIGIT})*
BINARY_INTEGER_LITERAL=0[Bb]({BINARY_DIGIT})+

FLOAT_LITERAL=(({FLOATING_POINT_LITERAL1})[Ff])|(({FLOATING_POINT_LITERAL2})[Ff])|(({FLOATING_POINT_LITERAL3})[Ff])|(({FLOATING_POINT_LITERAL4})[Ff])
DOUBLE_LITERAL=(({FLOATING_POINT_LITERAL1})[Ll]?)|(({FLOATING_POINT_LITERAL2})[Ll]?)|(({FLOATING_POINT_LITERAL3})[Ll]?)|(({FLOATING_POINT_LITERAL4})[Ll])
FLOATING_POINT_LITERAL1=({DIGIT})+"."({DIGIT})*({EXPONENT_PART})?
FLOATING_POINT_LITERAL2="."({DIGIT})+({EXPONENT_PART})?
FLOATING_POINT_LITERAL3=({DIGIT})+({EXPONENT_PART})
FLOATING_POINT_LITERAL4=({DIGIT})+
EXPONENT_PART=[Ee]["+""-"]?({DIGIT})*
HEX_FLOAT_LITERAL={HEX_SIGNIFICANT}{BINARY_EXPONENT}[Ff]
HEX_DOUBLE_LITERAL={HEX_SIGNIFICANT}{BINARY_EXPONENT}[Ll]?
BINARY_EXPONENT=[Pp][+-]?{DIGIT}+
HEX_SIGNIFICANT={HEX_INTEGER_LITERAL}|{HEX_INTEGER_LITERAL}.|0[Xx]{HEX_DIGIT}*.{HEX_DIGIT}+

CHARACTER_LITERAL=[L]?"'"([^\\\'\n]|{ESCAPE_SEQUENCE})*("'"|\\)?
STRING_LITERAL=\"([^\\\"\n]|{ESCAPE_SEQUENCE})*(\"|\\)?
ESCAPE_SEQUENCE=\\[^\n]|{EOL_ESC}

NON_EOL_WS=[\ \t]|{EOL_ESC}
PRP=# {NON_EOL_WS}*

DIRECTIVE_CONTENT=({NON_EOL}|{BLOCK_COMMENT})*

%state DIRECTIVE

%%


<YYINITIAL> {BLOCK_COMMENT} { return OCTokenTypes.BLOCK_COMMENT; }

<DIRECTIVE> {DIRECTIVE_CONTENT} {return OCTokenTypes.DIRECTIVE_CONTENT; }
<YYINITIAL, DIRECTIVE> ({WHITE_SPACE_CHAR}|{EOL_ESC})+ { yybegin(YYINITIAL); return TokenType.WHITE_SPACE; }

<YYINITIAL> {EOL_COMMENT} { return OCTokenTypes.EOL_COMMENT; }

<YYINITIAL> {INTEGER_LITERAL} { return OCTokenTypes.INTEGER_LITERAL; }

<YYINITIAL> {FLOAT_LITERAL}      { return OCTokenTypes.FLOAT_LITERAL; }
<YYINITIAL> {HEX_FLOAT_LITERAL}  { return OCTokenTypes.FLOAT_LITERAL; }
<YYINITIAL> {DOUBLE_LITERAL}     { return OCTokenTypes.FLOAT_LITERAL; }
<YYINITIAL> {HEX_DOUBLE_LITERAL} { return OCTokenTypes.FLOAT_LITERAL; }

<YYINITIAL> {CHARACTER_LITERAL} { return OCTokenTypes.CHARACTER_LITERAL; }
<YYINITIAL> "@" {STRING_LITERAL} { return OCTokenTypes.NSSTRING_LITERAL; }
<YYINITIAL> {STRING_LITERAL} { return OCTokenTypes.STRING_LITERAL; }

<YYINITIAL> {PRP} "if"          { yybegin(DIRECTIVE); return OCTokenTypes.IF_DIRECTIVE     ;}
<YYINITIAL> {PRP} "ifdef"       { yybegin(DIRECTIVE); return OCTokenTypes.IFDEF_DIRECTIVE  ;}
<YYINITIAL> {PRP} "ifndef"      { yybegin(DIRECTIVE); return OCTokenTypes.IFNDEF_DIRECTIVE ;}
<YYINITIAL> {PRP} "elif"        { yybegin(DIRECTIVE); return OCTokenTypes.ELIF_DIRECTIVE   ;}
<YYINITIAL> {PRP} "else"        { yybegin(DIRECTIVE); return OCTokenTypes.ELSE_DIRECTIVE   ;}
<YYINITIAL> {PRP} "endif"       { yybegin(DIRECTIVE); return OCTokenTypes.ENDIF_DIRECTIVE  ;}
<YYINITIAL> {PRP} "include"     { yybegin(DIRECTIVE); return OCTokenTypes.INCLUDE_DIRECTIVE;}
<YYINITIAL> {PRP} "include_next" { yybegin(DIRECTIVE); return OCTokenTypes.INCLUDE_NEXT_DIRECTIVE;}
<YYINITIAL> {PRP} "import"      { yybegin(DIRECTIVE); return OCTokenTypes.IMPORT_DIRECTIVE ;}
<YYINITIAL> {PRP} "define"      { yybegin(DIRECTIVE); return OCTokenTypes.DEFINE_DIRECTIVE ;}
<YYINITIAL> {PRP} "undef"       { yybegin(DIRECTIVE); return OCTokenTypes.UNDEF_DIRECTIVE  ;}
<YYINITIAL> {PRP} "line"        { yybegin(DIRECTIVE); return OCTokenTypes.LINE_DIRECTIVE   ;}
<YYINITIAL> {PRP} "error"       { yybegin(DIRECTIVE); return OCTokenTypes.ERROR_DIRECTIVE  ;}
<YYINITIAL> {PRP} "warning"     { yybegin(DIRECTIVE); return OCTokenTypes.WARNING_DIRECTIVE  ;}
<YYINITIAL> {PRP} "pragma"      { yybegin(DIRECTIVE); return OCTokenTypes.PRAGMA_DIRECTIVE ;}
<YYINITIAL> "#" {IDENTIFIER}    { return OCTokenTypes.UNKNOWN_DIRECTIVE; }

<YYINITIAL> "auto"       { return OCTokenTypes.AUTO_KEYWORD;       }
<YYINITIAL> "break"      { return OCTokenTypes.BREAK_KEYWORD;      }
<YYINITIAL> "case"       { return OCTokenTypes.CASE_KEYWORD;       }
<YYINITIAL> "char"       { return OCTokenTypes.CHAR_KEYWORD;       }
<YYINITIAL> "continue"   { return OCTokenTypes.CONTINUE_KEYWORD;   }
<YYINITIAL> "default"    { return OCTokenTypes.DEFAULT_KEYWORD;    }
<YYINITIAL> "do"         { return OCTokenTypes.DO_KEYWORD;         }
<YYINITIAL> "double"     { return OCTokenTypes.DOUBLE_KEYWORD;     }
<YYINITIAL> "else"       { return OCTokenTypes.ELSE_KEYWORD;       }
<YYINITIAL> "enum"       { return OCTokenTypes.ENUM_KEYWORD;       }
<YYINITIAL> "extern"     { return OCTokenTypes.EXTERN_KEYWORD;     }
<YYINITIAL> "float"      { return OCTokenTypes.FLOAT_KEYWORD;      }
<YYINITIAL> "for"        { return OCTokenTypes.FOR_KEYWORD;        }
<YYINITIAL> "goto"       { return OCTokenTypes.GOTO_KEYWORD;       }
<YYINITIAL> "if"         { return OCTokenTypes.IF_KEYWORD;         }

<YYINITIAL> "const"      { return OCTokenTypes.CONST_KEYWORD;      }
<YYINITIAL> "__const"    { return OCTokenTypes.CONST_KEYWORD;      }
<YYINITIAL> "__const__"  { return OCTokenTypes.CONST_KEYWORD;      }

<YYINITIAL> "inline"     { return OCTokenTypes.INLINE_KEYWORD;     }
<YYINITIAL> "__inline"   { return OCTokenTypes.INLINE_KEYWORD;     }
<YYINITIAL> "__inline__" { return OCTokenTypes.INLINE_KEYWORD;     }

<YYINITIAL> "signed"     { return OCTokenTypes.SIGNED_KEYWORD;     }
<YYINITIAL> "__signed"   { return OCTokenTypes.SIGNED_KEYWORD;     }
<YYINITIAL> "__signed__" { return OCTokenTypes.SIGNED_KEYWORD;     }

<YYINITIAL> "volatile"     { return OCTokenTypes.VOLATILE_KEYWORD;   }
<YYINITIAL> "__volatile"   { return OCTokenTypes.VOLATILE_KEYWORD;   }
<YYINITIAL> "__volatile__" { return OCTokenTypes.VOLATILE_KEYWORD;   }

<YYINITIAL> "int"        { return OCTokenTypes.INT_KEYWORD;        }
<YYINITIAL> "long"       { return OCTokenTypes.LONG_KEYWORD;       }
<YYINITIAL> "register"   { return OCTokenTypes.REGISTER_KEYWORD;   }
<YYINITIAL> "restrict"   { return OCTokenTypes.RESTRICT_KEYWORD;   }
<YYINITIAL> "__strong"   { return OCTokenTypes.STRONG_KEYWORD;     }
<YYINITIAL> "__weak"     { return OCTokenTypes.WEAK_KEYWORD;       }
<YYINITIAL> "__block"    { return OCTokenTypes.BLOCK_KEYWORD;      }
<YYINITIAL> "return"     { return OCTokenTypes.RETURN_KEYWORD;     }
<YYINITIAL> "short"      { return OCTokenTypes.SHORT_KEYWORD;      }
<YYINITIAL> "sizeof"     { return OCTokenTypes.SIZEOF_KEYWORD;     }
<YYINITIAL> "__alignof__" { return OCTokenTypes.ALIGNOF_KEYWORD;     }
<YYINITIAL> "__typeof__" { return OCTokenTypes.TYPEOF_KEYWORD;     }
<YYINITIAL> "static"     { return OCTokenTypes.STATIC_KEYWORD;     }
<YYINITIAL> "struct"     { return OCTokenTypes.STRUCT_KEYWORD;     }
<YYINITIAL> "switch"     { return OCTokenTypes.SWITCH_KEYWORD;     }
<YYINITIAL> "typedef"    { return OCTokenTypes.TYPEDEF_KEYWORD;    }
<YYINITIAL> "union"      { return OCTokenTypes.UNION_KEYWORD;      }
<YYINITIAL> "unsigned"   { return OCTokenTypes.UNSIGNED_KEYWORD;   }
<YYINITIAL> "void"       { return OCTokenTypes.VOID_KEYWORD;       }
<YYINITIAL> "while"      { return OCTokenTypes.WHILE_KEYWORD;      }
<YYINITIAL> "_Bool"      { return OCTokenTypes._BOOL_KEYWORD;      }
<YYINITIAL> "_Complex"   { return OCTokenTypes._COMPLEX_KEYWORD;   }
<YYINITIAL> "_Imaginary" { return OCTokenTypes._IMAGINARY_KEYWORD; }
<YYINITIAL> "__attribute__" { return OCTokenTypes.__ATTRIBUTE_KEYWORD; }
<YYINITIAL> "__asm"      { return OCTokenTypes.__ASM_KEYWORD; }

<YYINITIAL> "@interface"        { return OCTokenTypes.INTERFACE_KEYWORD     ; }
<YYINITIAL> "@implementation"   { return OCTokenTypes.IMPLEMENTATION_KEYWORD; }
<YYINITIAL> "@protocol"         { return OCTokenTypes.PROTOCOL_KEYWORD      ; }
<YYINITIAL> "@end"              { return OCTokenTypes.END_KEYWORD           ; }
<YYINITIAL> "@private"          { return OCTokenTypes.PRIVATE_KEYWORD       ; }
<YYINITIAL> "@protected"        { return OCTokenTypes.PROTECTED_KEYWORD     ; }
<YYINITIAL> "@public"           { return OCTokenTypes.PUBLIC_KEYWORD        ; }
<YYINITIAL> "@package"          { return OCTokenTypes.PACKAGE_KEYWORD       ; }
<YYINITIAL> "@optional"         { return OCTokenTypes.OPTIONAL_KEYWORD      ; }
<YYINITIAL> "@required"         { return OCTokenTypes.REQUIRED_KEYWORD      ; }
<YYINITIAL> "@try"              { return OCTokenTypes.TRY_KEYWORD           ; }
<YYINITIAL> "@throw"            { return OCTokenTypes.THROW_KEYWORD         ; }
<YYINITIAL> "@catch"            { return OCTokenTypes.CATCH_KEYWORD         ; }
<YYINITIAL> "@finally"          { return OCTokenTypes.FINALLY_KEYWORD       ; }
<YYINITIAL> "@class"            { return OCTokenTypes.CLASS_KEYWORD         ; }
<YYINITIAL> "@selector"         { return OCTokenTypes.SELECTOR_KEYWORD      ; }
<YYINITIAL> "@encode"           { return OCTokenTypes.ENCODE_KEYWORD        ; }
<YYINITIAL> "@synchronized"     { return OCTokenTypes.SYNCHRONIZED_KEYWORD  ; }
<YYINITIAL> "@property"         { return OCTokenTypes.PROPERTY_KEYWORD      ; }
<YYINITIAL> "@synthesize"       { return OCTokenTypes.SYNTHESIZE_KEYWORD    ; }
<YYINITIAL> "@dynamic"          { return OCTokenTypes.DYNAMIC_KEYWORD       ; }
<YYINITIAL> "@" {IDENTIFIER}    { return OCTokenTypes.UNKNOWN_AT_KEYWORD    ; }

<YYINITIAL> {IDENTIFIER} { return OCTokenTypes.IDENTIFIER; }

<YYINITIAL> "["          { return OCTokenTypes.LBRACKET  ; }
<YYINITIAL> "]"          { return OCTokenTypes.RBRACKET  ; }
<YYINITIAL> "{"          { return OCTokenTypes.LBRACE    ; }
<YYINITIAL> "}"          { return OCTokenTypes.RBRACE    ; }
<YYINITIAL> "("          { return OCTokenTypes.LPAR      ; }
<YYINITIAL> ")"          { return OCTokenTypes.RPAR      ; }
<YYINITIAL> "."          { return OCTokenTypes.DOT       ; }
<YYINITIAL> "->"         { return OCTokenTypes.DEREF     ; }
<YYINITIAL> "++"         { return OCTokenTypes.PLUSPLUS  ; }
<YYINITIAL> "--"         { return OCTokenTypes.MINUSMINUS; }
<YYINITIAL> "&"          { return OCTokenTypes.AND       ; }
<YYINITIAL> "*"          { return OCTokenTypes.MUL       ; }
<YYINITIAL> "+"          { return OCTokenTypes.PLUS      ; }
<YYINITIAL> "-"          { return OCTokenTypes.MINUS     ; }
<YYINITIAL> "~"          { return OCTokenTypes.TILDE     ; }
<YYINITIAL> "!"          { return OCTokenTypes.EXCL      ; }
<YYINITIAL> "/"          { return OCTokenTypes.DIV       ; }
<YYINITIAL> "%"          { return OCTokenTypes.PERC      ; }
<YYINITIAL> "<<"         { return OCTokenTypes.LTLT      ; }
<YYINITIAL> ">>"         { return OCTokenTypes.GTGT      ; }
<YYINITIAL> "<"          { return OCTokenTypes.LT        ; }
<YYINITIAL> ">"          { return OCTokenTypes.GT        ; }
<YYINITIAL> "<="         { return OCTokenTypes.LTEQ      ; }
<YYINITIAL> ">="         { return OCTokenTypes.GTEQ      ; }
<YYINITIAL> "=="         { return OCTokenTypes.EQEQ      ; }
<YYINITIAL> "!="         { return OCTokenTypes.EXCLEQ    ; }
<YYINITIAL> "^"          { return OCTokenTypes.XOR       ; }
<YYINITIAL> "|"          { return OCTokenTypes.OR        ; }
<YYINITIAL> "&&"         { return OCTokenTypes.ANDAND    ; }
<YYINITIAL> "||"         { return OCTokenTypes.OROR      ; }
<YYINITIAL> "?"          { return OCTokenTypes.QUEST     ; }
<YYINITIAL> ":"          { return OCTokenTypes.COLON     ; }
<YYINITIAL> ";"          { return OCTokenTypes.SEMICOLON ; }
<YYINITIAL> "..."        { return OCTokenTypes.ELLIPSIS  ; }
<YYINITIAL> "="          { return OCTokenTypes.EQ        ; }
<YYINITIAL> "*="         { return OCTokenTypes.MULTEQ    ; }
<YYINITIAL> "/="         { return OCTokenTypes.DIVEQ     ; }
<YYINITIAL> "%="         { return OCTokenTypes.PERCEQ    ; }
<YYINITIAL> "+="         { return OCTokenTypes.PLUSEQ    ; }
<YYINITIAL> "-="         { return OCTokenTypes.MINUSEQ   ; }
<YYINITIAL> "<<="        { return OCTokenTypes.LTLTEQ    ; }
<YYINITIAL> ">>="        { return OCTokenTypes.GTGTEQ    ; }
<YYINITIAL> "&="         { return OCTokenTypes.ANDEQ     ; }
<YYINITIAL> "^="         { return OCTokenTypes.XOREQ     ; }
<YYINITIAL> "|="         { return OCTokenTypes.OREQ      ; }
<YYINITIAL> ","          { return OCTokenTypes.COMMA     ; }
<YYINITIAL> "#"          { return OCTokenTypes.HASH      ; }
<YYINITIAL> "<#"         { return OCTokenTypes.TEMPLATE_START_MARK; }
<YYINITIAL> "#>"         { return OCTokenTypes.TEMPLATE_STOP_MARK; }
<YYINITIAL> "##"         { return OCTokenTypes.HASHHASH  ; }

<YYINITIAL, DIRECTIVE> . { yybegin(YYINITIAL); return TokenType.BAD_CHARACTER; }

