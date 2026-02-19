// RUN_PIPELINE_TILL: FRONTEND
fun foo(x: Int, y: Any): Int = x

fun <T> bar(x: T, y: Any): T = x

fun main() {
    val fooRef: (Int, Any) -> Unit = <!TYPE_MISMATCH!>::<!TYPE_MISMATCH!>foo<!><!>
    val barRef: (Int, Any) -> Unit = <!TYPE_MISMATCH!>::<!TYPE_MISMATCH!>bar<!><!>
}

/* GENERATED_FIR_TAGS: callableReference, functionDeclaration, functionalType, localProperty, nullableType,
propertyDeclaration, typeParameter */
