fun test(f: Foo) {
    for(i <caret>in f) {}
}

interface Foo

operator fun Int.iterator(): Any = Any()