// LANGUAGE: +NestedClassesInEnumEntryShouldBeInner

enum class E {
    ABC {
        <!WRONG_MODIFIER_TARGET!>enum<!> class F {
            DEF
        }
    }
}
