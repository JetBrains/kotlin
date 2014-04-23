// NEXT_SIBLING:
fun foo() {
    open class X(val x: Int)

    fun bar<T: X>(t: T): Int {
        return <selection>t.x + 1</selection>
    }
}