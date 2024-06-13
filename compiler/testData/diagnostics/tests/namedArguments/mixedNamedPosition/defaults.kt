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
    foo(p3 = 4.0, <!MIXING_NAMED_AND_POSITIONED_ARGUMENTS!>'4'<!>)

    foo(1, p3 = 2.0, <!MIXING_NAMED_AND_POSITIONED_ARGUMENTS!>""<!>)
    foo(1, p3 = 2.0, <!MIXING_NAMED_AND_POSITIONED_ARGUMENTS!>3.0<!>)
}
