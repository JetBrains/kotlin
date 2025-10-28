// RUN_PIPELINE_TILL: FRONTEND
// LATEST_LV_DIFFERENCE
// ISSUE: KT-81841

fun <T> foo1(b: (Any, T) -> Unit) { }
fun <T> foo2(b: (Int, T) -> Unit) { }
fun <T> foo3(b: (String, T) -> Unit) { }
fun of(vararg args: Any) {}
fun ofInt(vararg args: Int) {}
fun ofString(vararg args: String) {}

fun main() {
    foo1(::of) // Works in K1, though
    <!CANNOT_INFER_PARAMETER_TYPE!>foo1<!>(::<!INAPPLICABLE_CANDIDATE!>ofInt<!>)
    <!CANNOT_INFER_PARAMETER_TYPE!>foo1<!>(::<!INAPPLICABLE_CANDIDATE!>ofString<!>)

    foo2(::of)
    foo2(::ofInt)
    <!CANNOT_INFER_PARAMETER_TYPE!>foo2<!>(::<!INAPPLICABLE_CANDIDATE!>ofString<!>)

    foo3(::of)
    <!CANNOT_INFER_PARAMETER_TYPE!>foo3<!>(::<!INAPPLICABLE_CANDIDATE!>ofInt<!>)
    foo3(::ofString)
}

/* GENERATED_FIR_TAGS: functionDeclaration, functionalType, nullableType, typeParameter, vararg */
