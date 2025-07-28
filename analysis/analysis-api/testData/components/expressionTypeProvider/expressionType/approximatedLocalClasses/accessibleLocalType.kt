// APPROXIMATE_TYPE
// KTIJ-29913
fun foo() {
    open cla<caret>ss A

    fun bar(): Int {
        return <expr>object: A() {}</expr>
    }
}