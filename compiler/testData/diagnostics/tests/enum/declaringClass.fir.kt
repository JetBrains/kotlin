// FULL_JDK
// WITH_STDLIB
// LANGUAGE: -ProhibitEnumDeclaringClass

import java.util.*

enum class SomeEnum { A }

fun bar() {
    SomeEnum.A.<!UNRESOLVED_REFERENCE!>declaringClass<!>
}

fun <E : Enum<E>> foo(values: Array<E>) {
    EnumSet.noneOf(values.first().<!UNRESOLVED_REFERENCE!>declaringClass<!>)
    EnumSet.noneOf(values.first().<!UNRESOLVED_REFERENCE!>getDeclaringClass<!>())
}
