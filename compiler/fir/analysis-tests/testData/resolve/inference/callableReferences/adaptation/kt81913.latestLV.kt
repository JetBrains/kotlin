// RUN_PIPELINE_TILL: FRONTEND
// LATEST_LV_DIFFERENCE
// ISSUE: KT-81913

fun foo1(b: (Any, Any) -> Unit) { }

fun foo2(b: (Any, Array<String>) -> Unit) { }
fun foo22(b: (String, Array<String>) -> Unit) { }
fun foo23(b: (Any, Array<String>, Any) -> Unit) { }
fun foo24(b: (String, Array<String>, Any) -> Unit) { }

fun foo3(b: (Any, IntArray) -> Unit) { }
fun foo32(b: (Int, IntArray) -> Unit) { }
fun foo33(b: (Any, IntArray, Any) -> Unit) { }
fun foo34(b: (Int, IntArray, Int) -> Unit) { }

fun foo4(b: (Any, Array<Int>) -> Unit) { }
fun foo42(b: (Int, Array<Int>) -> Unit) { }
fun foo43(b: (Any, Array<Int>, Any) -> Unit) { }
fun foo44(b: (Int, Array<Int>, Int) -> Unit) { }

fun of(vararg args: Any) {}
fun ofString(vararg args: String) {}
fun ofInt(vararg args: Int) {}

fun main() {
    foo1(::of)
    foo1(::<!INAPPLICABLE_CANDIDATE!>ofString<!>)
    foo1(::<!INAPPLICABLE_CANDIDATE!>ofInt<!>)

    foo2(::of)
    foo2(::<!INAPPLICABLE_CANDIDATE!>ofString<!>)
    foo2(::<!INAPPLICABLE_CANDIDATE!>ofInt<!>)

    foo22(::of)
    foo22(::<!INAPPLICABLE_CANDIDATE!>ofString<!>)
    foo22(::<!INAPPLICABLE_CANDIDATE!>ofInt<!>)

    foo23(::of)
    foo23(::<!INAPPLICABLE_CANDIDATE!>ofString<!>)
    foo23(::<!INAPPLICABLE_CANDIDATE!>ofInt<!>)

    foo24(::of)
    foo24(::<!INAPPLICABLE_CANDIDATE!>ofString<!>)
    foo24(::<!INAPPLICABLE_CANDIDATE!>ofInt<!>)

    foo3(::of)
    foo3(::<!INAPPLICABLE_CANDIDATE!>ofString<!>)
    foo3(::<!INAPPLICABLE_CANDIDATE!>ofInt<!>)

    foo32(::of)
    foo32(::<!INAPPLICABLE_CANDIDATE!>ofString<!>)
    foo32(::<!INAPPLICABLE_CANDIDATE!>ofInt<!>)

    foo33(::of)
    foo33(::<!INAPPLICABLE_CANDIDATE!>ofString<!>)
    foo33(::<!INAPPLICABLE_CANDIDATE!>ofInt<!>)

    foo34(::of)
    foo34(::<!INAPPLICABLE_CANDIDATE!>ofString<!>)
    foo34(::<!INAPPLICABLE_CANDIDATE!>ofInt<!>)

    foo4(::of)
    foo4(::<!INAPPLICABLE_CANDIDATE!>ofString<!>)
    foo4(::<!INAPPLICABLE_CANDIDATE!>ofInt<!>)

    foo42(::of)
    foo42(::<!INAPPLICABLE_CANDIDATE!>ofString<!>)
    foo42(::<!INAPPLICABLE_CANDIDATE!>ofInt<!>)

    foo43(::of)
    foo43(::<!INAPPLICABLE_CANDIDATE!>ofString<!>)
    foo43(::<!INAPPLICABLE_CANDIDATE!>ofInt<!>)

    foo44(::of)
    foo44(::<!INAPPLICABLE_CANDIDATE!>ofString<!>)
    foo44(::<!INAPPLICABLE_CANDIDATE!>ofInt<!>)
}

/* GENERATED_FIR_TAGS: callableReference, functionDeclaration, functionalType, outProjection, vararg */
