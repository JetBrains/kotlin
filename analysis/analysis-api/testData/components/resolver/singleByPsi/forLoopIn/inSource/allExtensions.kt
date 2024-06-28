fun test(f: Foo) {
    for(i <caret>in f) {}
}

interface Foo
operator fun Foo.iterator(): Iterator = TODO()

interface Iterator

operator fun Iterator.next(): Any= TODO()
operator fun Iterator.hasNext(): Boolean = TODO()

