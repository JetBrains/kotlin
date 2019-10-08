package privateFieldInCompanion

class Foo {
    companion object {
        private const val PRIVATE_CONST = 1
        const val PUBLIC_CONST = 2

        private val PRIVATE_VAL = 3
        val PUBLIC_VAL = 4

        private val PRIVATE_STATIC = 5
        val PUBLIC_STATIC = 6
    }


    fun foo() {
        //Breakpoint!
        val a = 5
    }
}

fun main(args: Array<String>) {
    Foo().foo()
}

// EXPRESSION: PRIVATE_CONST
// RESULT: 1: I

// EXPRESSION: PUBLIC_CONST
// RESULT: 2: I

// EXPRESSION: PRIVATE_VAL
// RESULT: 3: I

// EXPRESSION: PUBLIC_VAL
// RESULT: 4: I

// EXPRESSION: PRIVATE_STATIC
// RESULT: 5: I

// EXPRESSION: PUBLIC_STATIC
// RESULT: 6: I
