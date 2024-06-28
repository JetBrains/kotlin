// FIR_IDENTICAL
// DIAGNOSTICS: -INVISIBLE_MEMBER -INVISIBLE_REFERENCE -UNUSED_PARAMETER

interface Parent
object ChildA : Parent
object ChildB : Parent

fun <@kotlin.internal.OnlyInputTypes T> select(a: T, b: T) {}

fun test() {
    <!TYPE_INFERENCE_ONLY_INPUT_TYPES_ERROR!>select<!>(ChildA, ChildB) // should be error
    select<Any>(ChildA, ChildB) // should be ok
}
