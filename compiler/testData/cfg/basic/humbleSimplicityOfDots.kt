fun Int.dot(): Int {
    return this + 1
}

fun String?.dot(): String = this ?: ""

fun i(): Int? = null

fun s(): String? = null

fun f() : Unit {
    2.toLong()
    3.equals(4)
    i()?.toLong()

    "test".length
    s().length()
}
