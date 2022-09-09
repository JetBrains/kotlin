// FULL_JDK
// WITH_STDLIB
// !LANGUAGE: -ProhibitEnumDeclaringClass

import java.util.*

enum class SomeEnum { A }

fun bar() {
    SomeEnum.A.declaringClass
}

fun <E : Enum<E>> foo(values: Array<E>) {
    EnumSet.noneOf(values.first().declaringClass)
    EnumSet.noneOf(values.first().getDeclaringClass())
}
