// COMPILER_ARGUMENTS: -Xcontext-parameters

/**
 * [x<caret_1>x]
 */
context(xx: String, xx: Int)
fun foo() {
    /**
     * [x<caret_2>x]
     */
}

/**
 * [x<caret_3>x]
 */
context(xx: Int)
context(xx: String)
fun foo() {
    /**
     * [x<caret_4>x]
     */
}