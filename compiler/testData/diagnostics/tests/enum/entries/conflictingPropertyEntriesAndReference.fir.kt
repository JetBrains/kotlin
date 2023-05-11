// !LANGUAGE: -EnumEntries
// WITH_STDLIB

enum class E {
    ;

    val entries: Int = 0
}

fun test() {
    E::entries
    val ref = E::entries
    val refType: (E) -> Int = E::entries
    val refTypeWithAnyExpectedType: Any = E::entries
}
