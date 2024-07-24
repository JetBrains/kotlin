package test

class Foo<T>(t: T) {
    val v<caret>alue = t
}

// CLASS_ID: kotlin/collections/Iterable
// IS_SUBTYPE: false
// IS_SUBTYPE_LENIENT: false
