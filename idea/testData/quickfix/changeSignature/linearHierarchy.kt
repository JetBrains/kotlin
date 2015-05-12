// "Add parameter to function 'f'" "true"
interface O {
    fun f(a: Int)
}

interface OO : O {
    override fun f(a: Int) {
    }
}

interface OOO : OO {
    override fun f(a: Int) {}
}

fun usage(o: O) {
    o.f(1)
}

fun usage(o: OO) {
    o.f(13, <caret>12)
}

fun usage(o: OOO) {
    o.f(3)
}
