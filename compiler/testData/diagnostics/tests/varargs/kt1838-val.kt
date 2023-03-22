// IGNORE_REVERSED_RESOLVE
// FIR_IDENTICAL
class A(vararg val t : Int) {
    init {
        val t1 : IntArray = t
    }
}
