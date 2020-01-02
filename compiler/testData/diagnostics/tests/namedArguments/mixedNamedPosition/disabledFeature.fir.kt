// !LANGUAGE: +NewInference -MixedNamedArgumentsInTheirOwnPosition
// !DIAGNOSTICS: -UNUSED_PARAMETER
// SKIP_TXT

fun foo(
    p1: Int,
    p2: String,
    p3: Double
) {}

fun main() {
    <!INAPPLICABLE_CANDIDATE!>foo<!>(p1 = 1, "2", 3.0)
    <!INAPPLICABLE_CANDIDATE!>foo<!>(1, p2 = "2", 3.0)
    foo(1, "2", p3 = 3.0)

    <!INAPPLICABLE_CANDIDATE!>foo<!>(p1 = 1, p2 = "2", 3.0)

    <!INAPPLICABLE_CANDIDATE!>foo<!>(1, p3 = 2.0, "")
    <!INAPPLICABLE_CANDIDATE!>foo<!>(1, p3 = 2.0, 3.0)
}
