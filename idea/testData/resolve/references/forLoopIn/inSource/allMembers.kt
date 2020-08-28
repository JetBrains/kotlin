fun test(f: Foo) {
    for(i <caret>in f) {}
}

interface Foo {
  fun iterator(): Iterator
}

interface Iterator {
  fun next(): Any
  fun hasNext(): Boolean
}

// MULTIRESOLVE
// REF: (in Foo).iterator()
// REF: (in Iterator).next()
// REF: (in Iterator).hasNext()