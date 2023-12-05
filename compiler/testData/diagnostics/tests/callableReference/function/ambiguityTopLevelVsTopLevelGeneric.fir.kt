fun foo(x: Int, y: Any): Int = x

fun <T> bar(x: T, y: Any): T = x

fun main() {
    val fooRef: (Int, Any) -> Unit = ::foo
    val barRef: (Int, Any) -> Unit = ::<!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>bar<!>
}
