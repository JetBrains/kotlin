// COMPILER_ARGUMENTS: -Xcontext-parameters

/**
 * [x<caret_1>x]
 */
context(xx: String)
fun foo() {
    /**
     * [x<caret_2>x]
     */
}

class A<xx> {
    /**
     * [x<caret_3>x]
     */
    context(xx: String)
    /**
     * [x<caret_4>x]
     */
    fun <xx> foo() {
        /**
         * [x<caret_5>x]
         */
    }
}