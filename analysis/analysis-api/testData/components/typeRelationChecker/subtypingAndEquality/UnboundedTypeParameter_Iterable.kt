package test

class Foo<T>(t: T) {
    val v<caret_type1>1 = t
}

val v<caret_type2>2: Iterable<String> = emptyList()

// ARE_EQUAL: false
// ARE_EQUAL_LENIENT: false
// IS_SUBTYPE: false
// IS_SUBTYPE_LENIENT: false
