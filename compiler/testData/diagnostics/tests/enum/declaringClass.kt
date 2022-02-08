// FULL_JDK
// WITH_STDLIB
// !LANGUAGE: -ProhibitEnumDeclaringClass

import java.util.*

fun <E : Enum<E>> foo(values: Array<E>) {
    EnumSet.noneOf(values.first().<!ENUM_DECLARING_CLASS_DEPRECATED_WARNING!>declaringClass<!>)
    EnumSet.noneOf(values.first().<!UNRESOLVED_REFERENCE!>getDeclaringClass<!>())
}
