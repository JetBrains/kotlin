// !DIAGNOSTICS: -UNUSED_VARIABLE

fun test() {
    arrayOf<List<String>>(listOf(""))
    arrayOf(listOf(""))

    arrayOf<Array<String>>(arrayOf(""))
    arrayOf(arrayOf(""))

    arrayOfNulls<Array<String>>(1)

    Array<Array<String>>(1) { arrayOf("") }
    Array(1) { arrayOf("") }
    Array(1) { arrayOf("") }

    emptyArray<Array<String>>()
    val x: Array<Array<String>> = emptyArray()
}
