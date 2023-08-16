// FIR_IDENTICAL
// !DIAGNOSTICS: -DEBUG_INFO_SMARTCAST

fun getArray(): Array<Int> = throw Exception()
fun getList(): MutableList<Int> = throw Exception()
fun getNullableList(): MutableList<Int>? = throw Exception()
fun fn() {
    getArray()[1] = 2
    getList()[1] = 2
    getArray()[1]++
    getList()[1]++
    getArray()[1] += 2
    getList()[1] += 2

    val nullable = getNullableList()
    if (nullable != null) {
        nullable[1] += 1
    }
}