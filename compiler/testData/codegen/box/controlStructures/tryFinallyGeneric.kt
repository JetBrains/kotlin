// IGNORE_BACKEND_FIR: JVM_IR
// TARGET_BACKEND: JVM
// WITH_RUNTIME
// !LANGUAGE: +NewInference

val stack = mutableListOf<Int>()

fun <E> MutableList<E>.pop() = this.removeAt(size - 1)

fun foo() {}

fun getBoolean(): Boolean = true

fun box(): String {
    val b = getBoolean()
    if (b) {
        stack.add(1)
        try {
            return "OK"
        } finally {
            stack.pop()
        }
    } else {
        return "OK"
    }
}