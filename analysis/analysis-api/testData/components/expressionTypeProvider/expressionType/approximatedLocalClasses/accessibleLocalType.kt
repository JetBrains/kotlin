// APPROXIMATE_TYPE
// KTIJ-29913
fun foo() {
    open class A

    fun bar(): Int {
        return <expr>object: A() {}</expr>
    }
}