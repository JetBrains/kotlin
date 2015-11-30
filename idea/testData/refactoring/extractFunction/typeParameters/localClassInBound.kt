// SIBLING:
fun foo() {
    open class X(val x: Int)

    fun <T: X> bar(t: T): Int {
        return <selection>t.x + 1</selection>
    }
}