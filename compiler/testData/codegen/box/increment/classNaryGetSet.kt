// IGNORE_BACKEND_FIR: JVM_IR
object A {
    var x = 0

    operator fun get(i1: Int, i2: Int, i3: Int): Int = x

    operator fun set(i1: Int, i2: Int, i3: Int, value: Int) {
        x = value
    }
}

fun box(): String {
    A.x = 0
    val xx = A[1, 2, 3]++
    return if (xx != 0 || A.x != 1) "Failed" else "OK"
}
