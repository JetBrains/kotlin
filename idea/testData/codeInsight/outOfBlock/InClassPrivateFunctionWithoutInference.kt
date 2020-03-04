// OUT_OF_CODE_BLOCK: TRUE

// as it's a class body not a named function body

class A {
    fun foo(): Int = 12

    private fun bar(): Int = foo() + <caret>
}

// TYPE: 1
// SKIP_ANALYZE_CHECK