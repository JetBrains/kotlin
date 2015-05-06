// "Suppress 'UNNECESSARY_NOT_NULL_ASSERTION' for statement " "true"

fun foo() {
    @suppress("UNNECESSARY_NOT_NULL_ASSERTION")
    for (i in list()<caret>!!) {}
}

fun list(): List<Int> = throw Exception()