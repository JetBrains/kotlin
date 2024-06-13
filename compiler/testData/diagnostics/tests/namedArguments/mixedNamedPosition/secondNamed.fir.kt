// LANGUAGE: +MixedNamedArgumentsInTheirOwnPosition
// DIAGNOSTICS: -UNUSED_PARAMETER
// SKIP_TXT

fun foo(a: String, b: String) {}

fun reformat(
    str: String,
    normalizeCase: String = "default",
    upperCaseFirstLetter: Boolean = true,
    divideByCamelHumps: Boolean = false,
    wordSeparator: Char = ' '
) {}

fun main() {
    <!INAPPLICABLE_CANDIDATE!>foo<!>(b = "first", a = "a", "second") // prints "a, second"
    <!INAPPLICABLE_CANDIDATE!>reformat<!>(normalizeCase = "first",str = "","second",false,true, 's' )
}

