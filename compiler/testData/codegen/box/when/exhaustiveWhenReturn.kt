// WITH_STDLIB
enum class A { V }

fun box(): String {
    val a: A = A.V
    when (a) {
        A.V -> return "OK"
    }
}