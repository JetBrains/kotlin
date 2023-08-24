// !LANGUAGE: +EnumEntries -PrioritizedEnumEntries
// WITH_STDLIB

enum class Some {}

val x = Some::<!DEPRECATED_ACCESS_TO_ENUM_ENTRY_PROPERTY_AS_REFERENCE!>entries<!>
