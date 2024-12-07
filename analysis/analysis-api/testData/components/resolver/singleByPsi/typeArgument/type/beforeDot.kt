// DISABLE_DEPENDED_MODE

class Generic<T> {
    inner class Nested
}

class C {
    val prop: Generic<<caret>Foo>.Nested? = null
}

class Foo


