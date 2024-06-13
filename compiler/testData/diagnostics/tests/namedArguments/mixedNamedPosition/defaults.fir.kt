// LANGUAGE: +MixedNamedArgumentsInTheirOwnPosition
// DIAGNOSTICS: -UNUSED_PARAMETER
// SKIP_TXT

fun foo(
    p1: Int = 1,
    p2: String = "",
    p3: Double = 3.0,
    p4: Char = '4'
) {}

fun main() {
    foo(p1 = 1, "2", 3.0)
    foo(1, p2 = "2", 3.0)
    foo(1, "2", p3 = 3.0)

    foo(p1 = 1)
    foo(1, p2 = "2")

    foo(p1 = 1, p2 = "2", 3.0)

    foo()
    foo(1, p2 = "")
    <!INAPPLICABLE_CANDIDATE!>foo<!>(p3 = 4.0, '4')

    <!INAPPLICABLE_CANDIDATE!>foo<!>(1, p3 = 2.0, "")
    <!INAPPLICABLE_CANDIDATE!>foo<!>(1, p3 = 2.0, 3.0)
}
