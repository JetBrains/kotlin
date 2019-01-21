// !WITH_NEW_INFERENCE
// NI_EXPECTED_FILE
// See KT-13401: SOE in VarianceChecker

interface Rec<T: Rec<T>> {
    fun t(): T
}
interface Super<out U> {
    fun foo(p: Rec<*>) = p.t()
}
// Related variance errors
class Owner<in T> {
    inner class Inner<U : <!TYPE_VARIANCE_CONFLICT!>T<!>>(val u: U) {
        fun getT() = u
    }

    fun foo(arg: Inner<*>) = arg.getT()
}