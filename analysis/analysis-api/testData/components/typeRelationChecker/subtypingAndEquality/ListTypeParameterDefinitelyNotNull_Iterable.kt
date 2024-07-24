package test

class Foo<T : List<String>?>(t: T & Any) {
    val v<caret_type1>1 = t
}

val v<caret_type2>2: Iterable<String> = emptyList()

// ARE_EQUAL: false
// ARE_EQUAL_LENIENT: false
// IS_SUBTYPE: true
// IS_SUBTYPE_LENIENT: true
