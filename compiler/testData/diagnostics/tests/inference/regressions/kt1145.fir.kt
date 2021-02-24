// !CHECK_TYPE

//KT-1145 removing explicit generics on a call to Iterable<T>.map(...) seems to generate an odd bytecode/runtime error

package d

import checkSubtype

fun test(numbers: Iterable<Int>) {
    val s = numbers.map{it.toString()}.fold(""){it, it2 -> it + it2}
    <!INAPPLICABLE_CANDIDATE!>checkSubtype<!><Int>(s)
}

//from library
fun <T, R> Iterable<T>.map(transform : (T) -> R) : List<R> {}

fun <T> Iterable<T>.fold(initial: T, operation: (T, T) -> T): T {}
