// COMPILATION_ERRORS

class Outer {
    class Nested
}

fun fo<caret>o(): Outer.Nested.Unknown? = null
