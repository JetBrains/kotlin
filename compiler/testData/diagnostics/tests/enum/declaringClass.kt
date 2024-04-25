// FULL_JDK
// WITH_STDLIB
// LANGUAGE: -ProhibitEnumDeclaringClass

import java.util.*

enum class SomeEnum { A }

fun bar() {
    SomeEnum.A.<!ENUM_DECLARING_CLASS_DEPRECATED_WARNING!>declaringClass<!>
}

fun <E : Enum<E>> foo(values: Array<E>) {
    EnumSet.noneOf(values.first().<!ENUM_DECLARING_CLASS_DEPRECATED_WARNING!>declaringClass<!>)
    EnumSet.noneOf(values.first().<!UNRESOLVED_REFERENCE!>getDeclaringClass<!>())
}
