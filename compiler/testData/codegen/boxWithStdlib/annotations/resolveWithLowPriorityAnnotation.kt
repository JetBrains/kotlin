@Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
@kotlin.internal.LowPriorityInOverloadResolution
fun foo(i: Int) = 1

fun foo(a: Any) = 2

@Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
fun <T> Iterable<T>.contains1(element: @kotlin.internal.NoInfer T): Boolean = false

@kotlin.jvm.JvmName("containsAny")
@Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
@kotlin.internal.LowPriorityInOverloadResolution
fun <T> Iterable<T>.contains1(element: T): Boolean = true

fun main(args: Array<String>) {
    println(box())
}
fun box(): String {
    if (foo(1) != 2) return "fail"
    val l = listOf(1, 2)
    val i: Int? = 42
    val a: Any = ""
    return when {
        l.contains1(3) -> "fail0"
        !l.contains1(i) -> "fail1"
        !l.contains1(a) -> "fail2"
        !l.contains1("") -> "fail3"
        else -> "OK"
    }
}