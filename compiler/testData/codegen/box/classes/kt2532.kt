// IGNORE_BACKEND_FIR: JVM_IR
package foo

interface B {
    val c: Int
        get() = 2
}

class A(val b: B) : B by b {
    override val c: Int = 3
}

fun box(): String {
    val c = A(object: B {}).c
    return if (c == 3) "OK" else "fail: $c"
}
