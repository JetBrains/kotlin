// !LANGUAGE: -UnderscoresInNumericLiterals
// !DIAGNOSTICS: -UNUSED_EXPRESSION

fun foo() {
    100_1
    <!ILLEGAL_UNDERSCORE!>3_.1<!>
    2___4
    <!ILLEGAL_UNDERSCORE!>123_<!>
}