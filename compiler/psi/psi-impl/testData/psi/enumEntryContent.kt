// COMPILATION_ERRORS

enum class MyEnumClass {
    ENTRY {
        class NestedClass {
            class NestedNested {}
        }

        object NestedObject {
            object NestedNested {}
        }

        init {
            fun initFunction() {}
            class LocalClass {}
            object LocalObject {}
            val objectLiteral = object {
                fun literalFunction() {}
                var literalVariable = 25
            }
        }

        fun foo() {

        }

        val baz: Int
    }
}
