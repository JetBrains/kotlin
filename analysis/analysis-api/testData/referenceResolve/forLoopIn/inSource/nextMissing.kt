fun test(f: Foo) {
    for(i <caret>in f) {}
}

interface Foo {
  fun iterator(): Iterator
}

interface Iterator {
  fun hasNext(): Boolean
}

// MULTIRESOLVE
