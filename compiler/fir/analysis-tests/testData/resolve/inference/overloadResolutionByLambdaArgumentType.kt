// ISSUE: KT-43619

class A<K>
fun <K> A<K>.foo(k: K) = k // (1)
fun <K> A<K>.foo(a: A<K>.() -> Unit) = 2 // (2)

fun test(){
    A<Int>().foo {} // (1)
    A<Int>().foo<Int> {} // (1)
    A<Int>().<!NONE_APPLICABLE!>foo<!><Any> {} // error
}
