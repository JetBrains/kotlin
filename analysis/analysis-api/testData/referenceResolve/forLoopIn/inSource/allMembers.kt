fun test(f: Foo) {
    for(i <caret>in f) {}
}

interface Foo {
    operator fun iterator(): Iterator
}

interface Iterator {
  operator fun next(): Any
  operator fun hasNext(): Boolean
}

// MULTIRESOLVE
