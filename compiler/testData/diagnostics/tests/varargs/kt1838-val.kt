// FIR_IDENTICAL
// !DIAGNOSTICS: -UNUSED_VARIABLE
class A(vararg val t : Int) {
    init {
        val t1 : IntArray = t
    }
}
