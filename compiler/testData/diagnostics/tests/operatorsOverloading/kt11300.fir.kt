// !WITH_NEW_INFERENCE

class A {
    operator fun get(x: Int): Int = x
    fun set(x: Int, y: Int) {} // no `operator` modifier
}

fun main() {
    val a = A()
    a[1]++
    a[1] += 3
    a[1] = a[1] + 3
}