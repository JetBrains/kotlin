enum class E {
    ABC {
        <!NESTED_CLASS_NOT_ALLOWED_SINCE_1_3!>enum class F<!> {
            DEF
        }
    }
}
