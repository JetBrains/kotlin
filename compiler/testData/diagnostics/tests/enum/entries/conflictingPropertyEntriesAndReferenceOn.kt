// FIR_IDENTICAL
// !LANGUAGE: +EnumEntries -PrioritizedEnumEntries
// WITH_STDLIB
// FIR_DUMP

enum class E {
    ;

    val entries: Int = 0
}

fun test() {
    E::<!DEPRECATED_ACCESS_TO_ENUM_ENTRY_PROPERTY_AS_REFERENCE!>entries<!>
    val ref = E::<!DEPRECATED_ACCESS_TO_ENUM_ENTRY_PROPERTY_AS_REFERENCE!>entries<!>
    val refType: (E) -> Int = E::entries
    val refTypeWithAnyExpectedType: Any = E::<!DEPRECATED_ACCESS_TO_ENUM_ENTRY_PROPERTY_AS_REFERENCE!>entries<!>
}
