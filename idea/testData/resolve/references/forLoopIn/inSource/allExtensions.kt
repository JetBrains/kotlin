fun test(f: Foo) {
    for(i <caret>in f) {}
}

trait Foo
fun Foo.iterator(): Iterator

trait Iterator

fun Iterator.next(): Any
fun Iterator.hasNext(): Boolean

// MULTIRESOLVE
// REF: (for Foo in <root>).iterator()
// REF: (for Iterator in <root>).next()
// REF: (for Iterator in <root>).hasNext()