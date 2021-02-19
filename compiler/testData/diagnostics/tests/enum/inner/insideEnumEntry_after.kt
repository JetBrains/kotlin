// FIR_IDENTICAL
// !LANGUAGE: +NestedClassesInEnumEntryShouldBeInner

enum class E {
    ABC {
        enum class F {
            DEF
        }
    }
}
