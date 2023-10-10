// FIR_IDENTICAL
// ISSUE: KT-62473
// WITH_STDLIB

class A(val list: List<*>)

fun test(a: A) {
    val result = mutableListOf<Int>()
    @Suppress("UNCHECKED_CAST")
    result += (a.list as List<Int>).filter { it > 0 }
}
