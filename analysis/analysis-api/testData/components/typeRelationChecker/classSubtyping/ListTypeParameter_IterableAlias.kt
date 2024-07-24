package test

typealias IterableAlias<T> = Iterable<T>

class Foo<T : List<String>>(t: T) {
    val v<caret>alue = t
}

// CLASS_ID: test/IterableAlias
// IS_SUBTYPE: false
// IS_SUBTYPE_LENIENT: false
