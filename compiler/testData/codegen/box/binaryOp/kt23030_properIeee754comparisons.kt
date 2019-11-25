// !LANGUAGE: +ProperIeee754Comparisons
// IGNORE_BACKEND_FIR: JVM_IR
class C {
    operator fun Int.compareTo(c: Char) = 0

    fun foo(x: Int, y: Char): String {
        if (x < y) {
            throw Error()
        }
        return "${y}K"
    }
}

fun box(): String {
    return C().foo(42, 'O')
}