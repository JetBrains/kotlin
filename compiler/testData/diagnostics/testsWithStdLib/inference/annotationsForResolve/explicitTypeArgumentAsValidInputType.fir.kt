// !DIAGNOSTICS: -INVISIBLE_MEMBER -INVISIBLE_REFERENCE -UNUSED_PARAMETER
// !LANGUAGE: -NonStrictOnlyInputTypesChecks

interface Parent
object ChildA : Parent
object ChildB : Parent

fun <<!HIDDEN!>@kotlin.internal.OnlyInputTypes<!> T> select(a: T, b: T) {}

fun test() {
    select(ChildA, ChildB) // should be error
    select<Any>(ChildA, ChildB) // should be ok
}
