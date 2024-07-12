// WITH_STDLIB

fun test(): Int = buildList {
    add(1)
    add(2)

    val number = removeLastOrNull() ?: throw Exception()
}.singleOrNull() ?: throw Exception()

fun box(): String {
    return if (test() == 1) "OK" else "NOK"
}
