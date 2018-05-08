// !LANGUAGE: +ProperIeee754Comparisons
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