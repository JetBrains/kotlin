// ISSUE: KT-65584

fun <T> giveItName(it: T, block: (myName: T) -> Unit) = block(it)

fun <T> duplicateIt(it: T, block: (T, T) -> Unit) = block

class MyTriple<T, K, M>(val a: T, val b: K, val c: M)

fun test() {
    giveItName(10) {
        MyTriple(it, it, it).also { self -> }
        (duplicateIt(it) { a, b -> }).also { <!DUPLICATE_PARAMETER_NAME_IN_FUNCTION_TYPE!>function<!> -> }
    }
}
