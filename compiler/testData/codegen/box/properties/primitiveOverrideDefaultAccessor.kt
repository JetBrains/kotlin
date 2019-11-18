// IGNORE_BACKEND_FIR: JVM_IR
interface R<T: Comparable<T>> {
    var value: T
}

class A(override var value: Int): R<Int>

fun box(): String {
    val a = A(239)
    a.value = 42
    return if (a.value == 42) "OK" else "Fail 1"
}
