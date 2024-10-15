// RUN_PIPELINE_TILL: SOURCE
// LANGUAGE: -NestedClassesInEnumEntryShouldBeInner

enum class E {
    ABC {
        <!NESTED_CLASS_DEPRECATED!>enum class F<!> {
            DEF
        }
    }
}
