// NEXT_SIBLING:
fun foo() {
    open class X(val x: Int)

    fun bar<T>(t: T): Int where T: X {
        return <selection>t.x + 1</selection>
    }
}