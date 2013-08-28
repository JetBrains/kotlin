fun test(f: Foo) {
    for(i <caret>in f) {}
}

trait Foo {
  fun iterator(): Iterator
}

trait Iterator {
  fun next(): Any
  fun hasNext(): Boolean
}

// MULTIRESOLVE
// REF: (in Foo).iterator()
// REF: (in Iterator).next()
// REF: (in Iterator).hasNext()