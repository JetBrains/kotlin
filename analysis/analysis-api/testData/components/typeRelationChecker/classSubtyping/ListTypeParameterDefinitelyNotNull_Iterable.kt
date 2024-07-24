package test

class Foo<T : List<String>?>(t: T & Any) {
    val v<caret>alue = t
}

// CLASS_ID: kotlin/collections/Iterable
// IS_SUBTYPE: false
// IS_SUBTYPE_LENIENT: false
