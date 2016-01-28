class A<T>
fun <T> A<T>.foo(): Collection<T> = null!!

fun main(a: A<*>, a1: A<out CharSequence>) {
    // see KT-9571
    for (i in a.foo()) { }
    for (i: Any? in a.foo()) { }

    for (i in a1.foo()) { }
    for (i: CharSequence in a1.foo()) { }
}
