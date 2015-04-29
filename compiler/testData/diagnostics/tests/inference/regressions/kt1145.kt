// !CHECK_TYPE

//KT-1145 removing explicit generics on a call to Iterable<T>.map(...) seems to generate an odd bytecode/runtime error

package d

fun test(numbers: Iterable<Int>) {
    val s = numbers.map{it.toString()}.fold(""){it, it2 -> it + it2}
    checkSubtype<Int>(<!TYPE_MISMATCH!>s<!>)
}

//from library
fun <T, R> Iterable<T>.map(<!UNUSED_PARAMETER!>transform<!> : (T) -> R) : List<R> {<!NO_RETURN_IN_FUNCTION_WITH_BLOCK_BODY!>}<!>

fun <T> Iterable<T>.fold(<!UNUSED_PARAMETER!>initial<!>: T, <!UNUSED_PARAMETER!>operation<!>: (T, T) -> T): T {<!NO_RETURN_IN_FUNCTION_WITH_BLOCK_BODY!>}<!>