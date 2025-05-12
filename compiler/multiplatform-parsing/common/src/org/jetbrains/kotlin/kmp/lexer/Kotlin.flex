@file:Suppress(
    "PARAMETER_NAME_CHANGED_ON_OVERRIDE", // TODO: KT-77206 (Remove after fixing https://github.com/JetBrains/intellij-deps-jflex/issues/8)
    "PackageDirectoryMismatch", // The generated files are located in another directory
)

package org.jetbrains.kotlin.kmp.lexer

import fleet.com.intellij.platform.syntax.SyntaxElementType
import fleet.com.intellij.platform.syntax.element.SyntaxTokenTypes
import fleet.com.intellij.platform.syntax.util.lexer.FlexLexer
import org.jetbrains.annotations.ApiStatus
import kotlin.jvm.JvmStatic // Not needed on JVM, but needed when compiling other targets

@ApiStatus.Experimental
%%

%{
  private class State(val state: Int, val lBraceCount: Int, val requiredInterpolationPrefix: Int) {
      override fun toString(): String {
          return "yystate = $state" +
                  (if (lBraceCount == 0) "" else "lBraceCount = $lBraceCount") +
                  (if (requiredInterpolationPrefix == -1) "" else "requiredInterpolationPrefix = $requiredInterpolationPrefix")
      }
  }

  private val states: MutableList<State> = mutableListOf()
  private var lBraceCount = 0
  private var requiredInterpolationPrefix = 0

  private var commentStart = 0
  private var commentDepth = 0

  private fun pushState(state: Int) {
      states.add(State(yystate(), lBraceCount, requiredInterpolationPrefix))
      lBraceCount = 0
      requiredInterpolationPrefix = -1
      yybegin(state)
  }

  private fun pushInterpolationPrefix(interpolationPrefix: Int) {
      states.add(State(yystate(), lBraceCount, requiredInterpolationPrefix))
      lBraceCount = 0
      requiredInterpolationPrefix = interpolationPrefix
      yybegin(STRING_PREFIX)
  }

  private fun popState() {
      val state: State = states.removeLast()
      lBraceCount = state.lBraceCount
      requiredInterpolationPrefix = state.requiredInterpolationPrefix
      yybegin(state.state)
  }

  private fun commentStateToTokenType(state: Int): SyntaxElementType {
    return when (state) {
      BLOCK_COMMENT -> KtTokens.BLOCK_COMMENT
      DOC_COMMENT -> KtTokens.DOC_COMMENT
      else -> throw IllegalArgumentException("Unexpected state: $state")
    }
  }
%}

%unicode
%class KotlinFlexLexer
%implements FlexLexer
%function advance
%type SyntaxElementType

%xstate STRING_PREFIX STRING RAW_STRING SHORT_TEMPLATE_ENTRY BLOCK_COMMENT DOC_COMMENT
%state LONG_TEMPLATE_ENTRY UNMATCHED_BACKTICK

DIGIT=[0-9]
DIGIT_OR_UNDERSCORE = [_0-9]
DIGITS = {DIGIT} {DIGIT_OR_UNDERSCORE}*
HEX_DIGIT=[0-9A-Fa-f]
HEX_DIGIT_OR_UNDERSCORE = [_0-9A-Fa-f]
WHITE_SPACE_CHAR=[\ \n\t\f]

// TODO: prohibit '$' in identifiers?
LETTER = [:letter:]|_
IDENTIFIER_PART=[:digit:]|{LETTER}
PLAIN_IDENTIFIER={LETTER} {IDENTIFIER_PART}*
// TODO: this one MUST allow everything accepted by the runtime
// TODO: Replace backticks with one backslash at the beginning
ESCAPED_IDENTIFIER = `[^`\n]+`
IDENTIFIER = {PLAIN_IDENTIFIER}|{ESCAPED_IDENTIFIER}
FIELD_IDENTIFIER = \${IDENTIFIER}

EOL_COMMENT="/""/"[^\n]*
SHEBANG_COMMENT="#!"[^\n]*

INTEGER_LITERAL={DECIMAL_INTEGER_LITERAL}|{HEX_INTEGER_LITERAL}|{BIN_INTEGER_LITERAL}
DECIMAL_INTEGER_LITERAL=(0|([1-9]({DIGIT_OR_UNDERSCORE})*)){TYPED_INTEGER_SUFFIX}
HEX_INTEGER_LITERAL=0[Xx]({HEX_DIGIT_OR_UNDERSCORE})*{TYPED_INTEGER_SUFFIX}
BIN_INTEGER_LITERAL=0[Bb]({DIGIT_OR_UNDERSCORE})*{TYPED_INTEGER_SUFFIX}
LONG_SUFFIX=[Ll]
UNSIGNED_SUFFIX=[Uu]
TYPED_INTEGER_SUFFIX = {UNSIGNED_SUFFIX}?{LONG_SUFFIX}?

//FLOAT_LITERAL=(({FLOATING_POINT_LITERAL1})[Ff])|(({FLOATING_POINT_LITERAL2})[Ff])|(({FLOATING_POINT_LITERAL3})[Ff])|(({FLOATING_POINT_LITERAL4})[Ff])
//DOUBLE_LITERAL=(({FLOATING_POINT_LITERAL1})[Dd]?)|(({FLOATING_POINT_LITERAL2})[Dd]?)|(({FLOATING_POINT_LITERAL3})[Dd]?)|(({FLOATING_POINT_LITERAL4})[Dd])
DOUBLE_LITERAL={FLOATING_POINT_LITERAL1}|{FLOATING_POINT_LITERAL2}|{FLOATING_POINT_LITERAL3}|{FLOATING_POINT_LITERAL4}
FLOATING_POINT_LITERAL1=({DIGITS})"."({DIGITS})+({EXPONENT_PART})?({FLOATING_POINT_LITERAL_SUFFIX})?
FLOATING_POINT_LITERAL2="."({DIGITS})({EXPONENT_PART})?({FLOATING_POINT_LITERAL_SUFFIX})?
FLOATING_POINT_LITERAL3=({DIGITS})({EXPONENT_PART})({FLOATING_POINT_LITERAL_SUFFIX})?
FLOATING_POINT_LITERAL4=({DIGITS})({FLOATING_POINT_LITERAL_SUFFIX})
FLOATING_POINT_LITERAL_SUFFIX=[Ff]
EXPONENT_PART=[Ee]["+""-"]?({DIGIT_OR_UNDERSCORE})*

CHARACTER_LITERAL="'"([^\\\'\n]|{ESCAPE_SEQUENCE})*("'"|\\)?
// TODO: introduce symbols (e.g. 'foo) as another way to write string literals
ESCAPE_SEQUENCE=\\(u{HEX_DIGIT}{HEX_DIGIT}{HEX_DIGIT}{HEX_DIGIT}|[^\n])

INTERPOLATION = \$+
// ANY_ESCAPE_SEQUENCE = \\[^]
THREE_QUO = (\"\"\")
THREE_OR_MORE_QUO = ({THREE_QUO}\"*)

REGULAR_STRING_PART=[^\\\"\n\$]+
SHORT_TEMPLATE_ENTRY={INTERPOLATION}{IDENTIFIER}
LONELY_DOLLAR=\$+
LONG_TEMPLATE_ENTRY_START={INTERPOLATION}\{
LONELY_BACKTICK=`

%%

// String templates

{INTERPOLATION}?\" {
            val interpolationPrefix = yylength() - 1
            pushInterpolationPrefix(maxOf(interpolationPrefix, 1))
            yypushback(1)
            if (interpolationPrefix != 0) return KtTokens.INTERPOLATION_PREFIX
}

<STRING_PREFIX> {THREE_QUO}      {
            yybegin(RAW_STRING)
            return KtTokens.OPEN_QUOTE
}
<RAW_STRING> \n                  { return KtTokens.REGULAR_STRING_PART }
<RAW_STRING> \"                  { return KtTokens.REGULAR_STRING_PART }
<RAW_STRING> \\                  { return KtTokens.REGULAR_STRING_PART }
<RAW_STRING> {THREE_OR_MORE_QUO} {
            val length = yytext().length
            if (length <= 3) { // closing """
                popState()
                return KtTokens.CLOSING_QUOTE
            } else { // some quotes at the end of a string, e.g. """ "foo""""
                yypushback(3) // return the closing quotes (""") to the stream
                return KtTokens.REGULAR_STRING_PART
            }
}

<STRING_PREFIX> \"          {
            yybegin(STRING)
            return KtTokens.OPEN_QUOTE
}
<STRING> \n                 {
            popState()
            yypushback(1)
            return KtTokens.DANGLING_NEWLINE
}
<STRING> \"                 {
            popState()
            return KtTokens.CLOSING_QUOTE
}
<STRING> {ESCAPE_SEQUENCE}  { return KtTokens.ESCAPE_SEQUENCE }

<STRING, RAW_STRING> {REGULAR_STRING_PART}         { return KtTokens.REGULAR_STRING_PART }
<STRING, RAW_STRING> {SHORT_TEMPLATE_ENTRY} {
            var interpolationPrefix = 0
            var i = 0
            while (i < yylength()) {
                if (yycharat(i) == '$') {
                    interpolationPrefix++
                } else {
                    break
                }
                i++
            }
            val rest = yylength() - interpolationPrefix
            if (interpolationPrefix == requiredInterpolationPrefix) {
                pushState(SHORT_TEMPLATE_ENTRY)
                yypushback(rest)
                return KtTokens.SHORT_TEMPLATE_ENTRY_START
            } else if (interpolationPrefix < requiredInterpolationPrefix) {
                yypushback(rest)
                return KtTokens.REGULAR_STRING_PART
            } else {
                yypushback(requiredInterpolationPrefix + rest)
                return KtTokens.REGULAR_STRING_PART
            }
}
// Only *this* keyword is itself an expression valid in this position
// *null*, *true* and *false* are also keywords and expression, but it does not make sense to put them
// in a string template for it'd be easier to just type them in without a dollar
<SHORT_TEMPLATE_ENTRY> "this"          {
            popState()
            return KtTokens.THIS_KEYWORD
}
<SHORT_TEMPLATE_ENTRY> {IDENTIFIER}    {
            popState()
            return KtTokens.IDENTIFIER
}

<STRING, RAW_STRING> {LONELY_DOLLAR}               { return KtTokens.REGULAR_STRING_PART }
<STRING, RAW_STRING> {LONG_TEMPLATE_ENTRY_START} {
            val interpolationPrefix = yylength() - 1
            if (interpolationPrefix == requiredInterpolationPrefix) {
                pushState(LONG_TEMPLATE_ENTRY)
                return KtTokens.LONG_TEMPLATE_ENTRY_START
            } else if (interpolationPrefix < requiredInterpolationPrefix) {
                yypushback(1)
                return KtTokens.REGULAR_STRING_PART
            } else {
                yypushback(requiredInterpolationPrefix + 1)
                return KtTokens.REGULAR_STRING_PART
            }
}
<LONG_TEMPLATE_ENTRY> "{"              {
            lBraceCount++
            return KtTokens.LBRACE
}
<LONG_TEMPLATE_ENTRY> "}"              {
            if (lBraceCount == 0) {
                popState()
                return KtTokens.LONG_TEMPLATE_ENTRY_END
            }
            lBraceCount--
            return KtTokens.RBRACE
}

// (Nested) comments

"/**/" {
    return KtTokens.BLOCK_COMMENT
}

"/**" {
            pushState(DOC_COMMENT)
            commentDepth = 0
            commentStart = getTokenStart()
}

"/*" {
            pushState(BLOCK_COMMENT)
            commentDepth = 0
            commentStart = getTokenStart()
}

<BLOCK_COMMENT, DOC_COMMENT> {
    "/*" {
            commentDepth++
    }

    <<EOF>> {
            val state = yystate()
            popState()
            zzStartRead = commentStart
            return commentStateToTokenType(state)
    }

    "*/" {
            if (commentDepth > 0) {
                commentDepth--
            }
            else {
                 val state = yystate()
                 popState()
                 zzStartRead = commentStart
                 return commentStateToTokenType(state)
            }
    }

    [\s\S] {}
}

// Mere mortals

({WHITE_SPACE_CHAR})+ { return KtTokens.WHITE_SPACE }

{EOL_COMMENT} { return KtTokens.EOL_COMMENT }
{SHEBANG_COMMENT} {
            if (zzCurrentPos == 0) {
                return KtTokens.SHEBANG_COMMENT
            }
            else {
                yypushback(yylength() - 1)
                return KtTokens.HASH
            }
}

{INTEGER_LITERAL}\.\. {
            yypushback(2)
            return KtTokens.INTEGER_LITERAL
}
{INTEGER_LITERAL} { return KtTokens.INTEGER_LITERAL }

{DOUBLE_LITERAL}     { return KtTokens.FLOAT_LITERAL }

{CHARACTER_LITERAL} { return KtTokens.CHARACTER_LITERAL }

"typealias"  { return KtTokens.TYPE_ALIAS_KEYWORD }
"interface"  { return KtTokens.INTERFACE_KEYWORD }
"continue"   { return KtTokens.CONTINUE_KEYWORD }
"package"    { return KtTokens.PACKAGE_KEYWORD }
"return"     { return KtTokens.RETURN_KEYWORD }
"object"     { return KtTokens.OBJECT_KEYWORD }
"while"      { return KtTokens.WHILE_KEYWORD }
"break"      { return KtTokens.BREAK_KEYWORD }
"class"      { return KtTokens.CLASS_KEYWORD }
"throw"      { return KtTokens.THROW_KEYWORD }
"false"      { return KtTokens.FALSE_KEYWORD }
"super"      { return KtTokens.SUPER_KEYWORD }
"typeof"     { return KtTokens.TYPEOF_KEYWORD }
"when"       { return KtTokens.WHEN_KEYWORD }
"true"       { return KtTokens.TRUE_KEYWORD }
"this"       { return KtTokens.THIS_KEYWORD }
"null"       { return KtTokens.NULL_KEYWORD }
"else"       { return KtTokens.ELSE_KEYWORD }
"try"        { return KtTokens.TRY_KEYWORD }
"val"        { return KtTokens.VAL_KEYWORD }
"var"        { return KtTokens.VAR_KEYWORD }
"fun"        { return KtTokens.FUN_MODIFIER }
"for"        { return KtTokens.FOR_KEYWORD }
"is"         { return KtTokens.IS_KEYWORD }
"in"         { return KtTokens.IN_MODIFIER }
"if"         { return KtTokens.IF_KEYWORD }
"do"         { return KtTokens.DO_KEYWORD }
"as"         { return KtTokens.AS_KEYWORD }

{FIELD_IDENTIFIER} { return KtTokens.FIELD_IDENTIFIER }
{IDENTIFIER} { return KtTokens.IDENTIFIER }
\!in{IDENTIFIER_PART}        {
            yypushback(3)
            return KtTokens.EXCL
}
\!is{IDENTIFIER_PART}        {
            yypushback(3)
            return KtTokens.EXCL
}

"..."        { return KtTokens.RESERVED }
"==="        { return KtTokens.EQEQEQ }
"!=="        { return KtTokens.EXCLEQEQEQ }
"!in"        { return KtTokens.NOT_IN }
"!is"        { return KtTokens.NOT_IS }
"as?"        { return KtTokens.AS_SAFE }
"++"         { return KtTokens.PLUSPLUS }
"--"         { return KtTokens.MINUSMINUS }
"<="         { return KtTokens.LTEQ }
">="         { return KtTokens.GTEQ }
"=="         { return KtTokens.EQEQ }
"!="         { return KtTokens.EXCLEQ }
"&&"         { return KtTokens.ANDAND }
"&"          { return KtTokens.AND }
"||"         { return KtTokens.OROR }
"*="         { return KtTokens.MULTEQ }
"/="         { return KtTokens.DIVEQ }
"%="         { return KtTokens.PERCEQ }
"+="         { return KtTokens.PLUSEQ }
"-="         { return KtTokens.MINUSEQ }
"->"         { return KtTokens.ARROW }
"=>"         { return KtTokens.DOUBLE_ARROW }
".."         { return KtTokens.RANGE }
"..<"        { return KtTokens.RANGE_UNTIL }
"::"         { return KtTokens.COLONCOLON }
"["          { return KtTokens.LBRACKET }
"]"          { return KtTokens.RBRACKET }
"{"          { return KtTokens.LBRACE }
"}"          { return KtTokens.RBRACE }
"("          { return KtTokens.LPAR }
")"          { return KtTokens.RPAR }
"."          { return KtTokens.DOT }
"*"          { return KtTokens.MUL }
"+"          { return KtTokens.PLUS }
"-"          { return KtTokens.MINUS }
"!"          { return KtTokens.EXCL }
"/"          { return KtTokens.DIV }
"%"          { return KtTokens.PERC }
"<"          { return KtTokens.LT }
">"          { return KtTokens.GT }
"?"          { return KtTokens.QUEST }
":"          { return KtTokens.COLON }
";;"         { return KtTokens.DOUBLE_SEMICOLON}
";"          { return KtTokens.SEMICOLON }
"="          { return KtTokens.EQ }
","          { return KtTokens.COMMA }
"#"          { return KtTokens.HASH }
"@"          { return KtTokens.AT }

{LONELY_BACKTICK} {
            pushState(UNMATCHED_BACKTICK)
            return SyntaxTokenTypes.BAD_CHARACTER
}

// error fallback
[\s\S]       { return SyntaxTokenTypes.BAD_CHARACTER }
// error fallback for exclusive states
<STRING, RAW_STRING, SHORT_TEMPLATE_ENTRY, BLOCK_COMMENT, DOC_COMMENT> .
             { return SyntaxTokenTypes.BAD_CHARACTER }

