// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +MixedNamedArgumentsInTheirOwnPosition +AllowAssigningArrayElementsToVarargsInNamedFormForFunctions
// DIAGNOSTICS: -UNUSED_PARAMETER
// SKIP_TXT

fun foo1(
    vararg p1: Int,
    p2: String,
    p3: Double
) {}

fun foo2(
    p1: Int,
    vararg p2: String,
    p3: Double
) {}

fun foo3(
    p1: Int,
    p2: String,
    vararg p3: Double
) {}

fun foo4(
    p1: Int,
    vararg p2: String,
    p3: Double,
    p4: Int
) {}

fun main() {
    foo1<!NO_VALUE_FOR_PARAMETER!>(1, 2, p2 = "3", <!MIXING_NAMED_AND_POSITIONAL_ARGUMENTS!>4.0<!>)<!>
    foo1(p1 = intArrayOf(1, 2), "3", p3 = 4.0)

    foo1<!NO_VALUE_FOR_PARAMETER!>(p2 = "3", <!MIXING_NAMED_AND_POSITIONAL_ARGUMENTS!>4.0<!>)<!>

    foo2(p1 = 1, "2", "3", p3 = 4.0)
    foo2(1, p2 = arrayOf("2", "3"), 4.0)
    foo2(1, p3 = 3.0)

    foo3(p1 = 1, "2", 3.0, 4.0)
    foo3(p1 = 1, "2", p3 = doubleArrayOf(3.0, 4.0))

    foo4<!NO_VALUE_FOR_PARAMETER!>(p1 = 1, "2", "3", p3 = 4.0, <!MIXING_NAMED_AND_POSITIONAL_ARGUMENTS!>5<!>)<!>
    foo4<!NO_VALUE_FOR_PARAMETER!>(1, "2", "3", p3 = 4.0, <!MIXING_NAMED_AND_POSITIONAL_ARGUMENTS!>5<!>)<!>
    foo4<!NO_VALUE_FOR_PARAMETER!>(1, p3 = 4.0, <!MIXING_NAMED_AND_POSITIONAL_ARGUMENTS!>5<!>)<!>

    foo1<!NO_VALUE_FOR_PARAMETER!>(1, 2, p3 = 3.0, <!MIXING_NAMED_AND_POSITIONAL_ARGUMENTS!>"4"<!>)<!>
    foo1(1, 2, p3 = 3.0, p2 = "4")
    foo1(*intArrayOf(1, 2), p3 = 3.0, p2 = "4")

    foo2(1, p3 = 2.0, <!MIXING_NAMED_AND_POSITIONAL_ARGUMENTS!>"4"<!>)
    foo2(1, p3 = 2.0, <!MIXING_NAMED_AND_POSITIONAL_ARGUMENTS!>*arrayOf("3", "4")<!>)
    foo2(1, p3 = 2.0, p2 = arrayOf("3", "4"))

    foo3<!NO_VALUE_FOR_PARAMETER!>(1, p3 = doubleArrayOf(2.0, 3.0), <!MIXING_NAMED_AND_POSITIONAL_ARGUMENTS!>"4"<!>)<!>
    foo3(1, p3 = doubleArrayOf(2.0, 3.0), p2 = "4")
}

/* GENERATED_FIR_TAGS: functionDeclaration, integerLiteral, stringLiteral, vararg */
