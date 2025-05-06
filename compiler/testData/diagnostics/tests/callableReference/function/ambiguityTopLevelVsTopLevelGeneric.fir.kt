// RUN_PIPELINE_TILL: FRONTEND
fun foo(x: Int, y: Any): Int = x

fun <T> bar(x: T, y: Any): T = x

fun main() {
    val fooRef: (Int, Any) -> Unit = ::foo
    val barRef: (Int, Any) -> Unit = ::<!CANNOT_INFER_PARAMETER_TYPE!>bar<!>
}
