// One-line comment
class Foo {

    // Several
    // lines
    // of
    // oneline
    // commentaries
    inner class Inner /* comment for constructor */ private constructor(x: Int) {
        /** dangling comment at the end of body*/
    } // Comment for closing bracket

    /*
    very
    long
    multiline
    comment
     */
    protected open fun foo(y: Int) { // Comment for opening bracket

    /* Comment for closing bracket */ }

    /**
     * Javadoc comment
     */
    val x: Int = 42
        // Comment for getter
        get() = field + 1
        // Dangling comment

    fun test() {
        foo(/* comment inside call */ 42)
    }
}