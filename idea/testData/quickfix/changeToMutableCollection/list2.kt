// "Change type to MutableList" "true"
// WITH_RUNTIME
fun main() {
    val list = foo()
    list[1]<caret> = 10
}

fun foo() = listOf(1, 2, 3)