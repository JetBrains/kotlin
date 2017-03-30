// ERROR: Cannot perform refactoring.\nInline Function is not supported for functions with return statements not at the end of the body.

class A {
    init {
        g()
    }

    fun <caret>g(): Int {
        try {
            return 0
        }
        catch (e: Error) {
            throw e
        }

    }
}