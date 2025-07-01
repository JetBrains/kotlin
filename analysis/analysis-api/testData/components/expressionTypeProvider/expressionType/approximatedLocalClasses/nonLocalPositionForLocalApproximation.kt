// APPROXIMATE_TYPE
// KTIJ-29913
object Someth<caret>ing


fun foo() {
    open class A

    fun bar(): Int {
        return <expr>object: A() {}</expr>
    }
}