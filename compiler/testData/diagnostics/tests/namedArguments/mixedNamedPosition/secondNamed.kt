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
    foo(b = "first", a = "a", <!MIXING_NAMED_AND_POSITIONED_ARGUMENTS!>"second"<!>) // prints "a, second"
    reformat(normalizeCase = "first",str = "",<!MIXING_NAMED_AND_POSITIONED_ARGUMENTS!>"second"<!>,<!MIXING_NAMED_AND_POSITIONED_ARGUMENTS!>false<!>,<!MIXING_NAMED_AND_POSITIONED_ARGUMENTS!>true<!>, <!MIXING_NAMED_AND_POSITIONED_ARGUMENTS!>'s'<!> )
}

