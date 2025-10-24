// RUN_PIPELINE_TILL: FRONTEND
// LATEST_LV_DIFFERENCE
// ISSUE: KT-81841

fun <T> foo(b: (Any, T) -> Unit) { }
fun of(vararg args: Any) {}

fun main() {
    foo(::of) // Works in K1, though
}

/* GENERATED_FIR_TAGS: functionDeclaration, functionalType, nullableType, typeParameter, vararg */
