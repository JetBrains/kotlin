// RUN_PIPELINE_TILL: SOURCE
// LANGUAGE: +NestedClassesInEnumEntryShouldBeInner

enum class E {
    ABC {
        <!NESTED_CLASS_NOT_ALLOWED!>enum class F<!> {
            DEF
        }
    }
}
