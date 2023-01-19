// !LANGUAGE: -EnumEntries
// WITH_STDLIB

enum class E {
    ;

    val entries: Int = 0
}

fun test() {
    E::<!OPT_IN_USAGE_ERROR!>entries<!>
    val ref = E::<!OPT_IN_USAGE_ERROR!>entries<!>
    val refType: (E) -> Int = E::entries
    val refTypeWithAnyExpectedType: Any = E::<!OPT_IN_USAGE_ERROR!>entries<!>
}
