// !LANGUAGE: -NestedClassesInEnumEntryShouldBeInner

enum class E {
    ABC {
        enum class F {
            DEF
        }
    }
}
