// !LANGUAGE: +InlineClasses
// IGNORE_BACKEND: JVM_IR

inline class Props(val intArray: IntArray) {
    val size get() = intArray.size

    fun foo(): Int {
        val a = size
        return a
    }
}

fun box(): String {
    val f = Props(intArrayOf(1, 2, 3))
    if (f.foo() != 3) return "fail"

    return "OK"
}