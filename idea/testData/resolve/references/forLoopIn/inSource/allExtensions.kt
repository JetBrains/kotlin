fun test(f: Foo) {
    for(i <caret>in f) {}
}

interface Foo
fun Foo.iterator(): Iterator

interface Iterator

fun Iterator.next(): Any
fun Iterator.hasNext(): Boolean

// MULTIRESOLVE
// REF: (for Foo in <root>).iterator()
// REF: (for Iterator in <root>).next()
// REF: (for Iterator in <root>).hasNext()