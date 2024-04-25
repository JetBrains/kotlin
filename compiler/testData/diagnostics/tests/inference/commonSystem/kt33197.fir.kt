// DIAGNOSTICS: -UNUSED_EXPRESSION

fun test(condition: Boolean) {
    val list1 =
        if (condition) mutableListOf<Int>()
        else emptyList()

    list1

    val list2 =
        if (condition) mutableListOf()
        else emptyList<Int>()

    list2
}

fun <T> mutableListOf(): MutableList<T> = TODO()
fun <T> emptyList(): List<T> = TODO()