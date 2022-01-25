fun test(f: Foo) {
    for(i <caret>in f) {}
}

interface Foo
fun Foo.iterator(): Iterator = TODO()

interface Iterator

fun Iterator.next(): Any= TODO()
fun Iterator.hasNext(): Boolean = TODO()

