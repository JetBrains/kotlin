// ISSUE: KT-69773
// WITH_STDLIB
// DIAGNOSTICS: -REDUNDANT_SPREAD_OPERATOR_IN_NAMED_FORM_IN_FUNCTION

fun foo(vararg bar: Int) = 0
val Int.fooVararg get() = "foo vararg"

fun bar(vararg bar: Int) = ""
val String.barVararg get() = "bar vararg"

@JvmName("baz")
fun bar(bar: IntArray) = 'A'
val Char.barIntArray get() = "bar IntArray"

fun duh(bar: IntArray) = false
val Boolean.duhIntArray get() = "duh IntArray"

fun main() {
    foo(bar = intArrayOf(1, 2)).fooVararg
    foo(bar = *intArrayOf(1, 2)).fooVararg
    foo(<!ARGUMENT_TYPE_MISMATCH!>intArrayOf(1, 2)<!>)
    foo(*intArrayOf(1, 2)).fooVararg
    foo(1, 2).fooVararg

    bar(bar = intArrayOf(1, 2)).barIntArray
    bar(bar = *intArrayOf(1, 2)).barVararg
    bar(intArrayOf(1, 2)).barIntArray
    bar(*intArrayOf(1, 2)).barVararg
    bar(1, 2).barVararg

    duh(bar = intArrayOf(1, 2)).duhIntArray
    duh(bar = <!NON_VARARG_SPREAD!>*<!>intArrayOf(1, 2))
    duh(intArrayOf(1, 2)).duhIntArray
    duh(<!NON_VARARG_SPREAD!>*<!>intArrayOf(1, 2))
    duh(<!ARGUMENT_TYPE_MISMATCH!>1<!>, <!TOO_MANY_ARGUMENTS!>2<!>)
}
