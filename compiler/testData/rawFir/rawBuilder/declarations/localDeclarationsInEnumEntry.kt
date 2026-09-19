enum class SomeEnum {
    A {
        init {
            fun foo() {}

            class Local {}
        }

        class Nested

        fun foo() {}
    }
}
