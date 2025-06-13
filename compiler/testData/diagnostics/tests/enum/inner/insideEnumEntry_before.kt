// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: -NestedClassesInEnumEntryShouldBeInner

enum class E {
    ABC {
        <!NESTED_CLASS_DEPRECATED!>enum class F<!> {
            DEF
        }
    }
}

/* GENERATED_FIR_TAGS: enumDeclaration, enumEntry */
