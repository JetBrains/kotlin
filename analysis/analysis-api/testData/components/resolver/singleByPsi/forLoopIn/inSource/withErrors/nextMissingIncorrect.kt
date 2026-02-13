fun test(f: Foo) {
    for(i <caret>in f) {}
}

interface Foo {
    operator fun iterator(): Iterator
}

interface Iterator {
    operator fun hasNext(): Boolean
}

operator fun Int.next(): Any= TODO()
operator fun String.next(): Any= TODO()