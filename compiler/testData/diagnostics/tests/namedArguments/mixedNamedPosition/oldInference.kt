// LANGUAGE: +MixedNamedArgumentsInTheirOwnPosition
// DIAGNOSTICS: -UNUSED_PARAMETER
// SKIP_TXT

fun foo(
    p1: Int,
    p2: String,
    p3: Double
) {}

fun main() {
    foo(p1 = 1, "2", 3.0)
    foo(1, p2 = "2", 3.0)
    foo(1, "2", p3 = 3.0)

    foo(p1 = 1, p2 = "2", 3.0)

    foo(1, p3 = 2.0, <!NO_VALUE_FOR_PARAMETER!><!MIXING_NAMED_AND_POSITIONED_ARGUMENTS!>""<!>)<!>
    foo(1, p3 = 2.0, <!NO_VALUE_FOR_PARAMETER!><!MIXING_NAMED_AND_POSITIONED_ARGUMENTS!>3.0<!>)<!>
}
