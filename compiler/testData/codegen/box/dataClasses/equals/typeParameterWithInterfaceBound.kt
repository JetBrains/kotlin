interface A

data class B<out T : A>(val a: T)

fun box(): String {
    val b1 = B(object : A {})
    val b2 = B(object : A {})
    return if (b1.equals(b2)) "Fail" else "OK"
}
