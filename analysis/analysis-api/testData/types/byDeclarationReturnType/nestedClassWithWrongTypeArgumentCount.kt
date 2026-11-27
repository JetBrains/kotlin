// COMPILATION_ERRORS

class Outer {
    class Nested<T>
}

fun fo<caret>o(): Outer.Nested? = null
