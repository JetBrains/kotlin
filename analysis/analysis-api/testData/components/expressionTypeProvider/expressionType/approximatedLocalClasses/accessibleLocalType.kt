// APPROXIMATE_TYPE
fun foo() {
    open class A

    fun bar(): Int {
        return <expr>object: A() {}</expr>
    }
}