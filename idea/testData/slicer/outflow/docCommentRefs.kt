// FLOW: OUT

class KotlinClass {
    fun <caret>foo(): Int = 10

    /**
     * Uses [foo]
     */
    fun bar() {
        val v = foo()
    }
}
