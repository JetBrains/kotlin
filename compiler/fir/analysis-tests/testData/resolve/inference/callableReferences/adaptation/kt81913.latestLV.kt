// RUN_PIPELINE_TILL: FRONTEND
// LATEST_LV_DIFFERENCE
// ISSUE: KT-81913

fun foo1(b: (Any, Any) -> Unit) { }
fun foo2(b: (Any, Array<String>) -> Unit) { }

fun of(vararg args: Any) {}

fun main() {
    foo1(::of)
    foo2(::of)
}

/* GENERATED_FIR_TAGS: callableReference, functionDeclaration, functionalType, outProjection, vararg */
