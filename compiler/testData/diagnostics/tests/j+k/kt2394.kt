//KT-2394 java.lang.Iterable<T> should be visible as jet.Iterable<out T>
package d

fun foo(iterable: Iterable<Int>, iterator: Iterator<Int>, comparable: Comparable<Any>) {
    iterable : Iterable<Any>
    iterator : Iterator<Any>
    comparable : Comparable<String>
}

fun bar(c: Collection<Int>) {
    c : Iterable<Any>
    c.iterator() : Iterator<Any>
}
