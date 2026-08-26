// LANGUAGE: +CollectionLiterals +CompanionBlocks
// WITH_STDLIB
fun <T> accept(set: Sequence<T>) {
}

fun test() {
    accept(<expr>[42]</expr>)
}
