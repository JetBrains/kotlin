// ISSUE: KT-63444
class A<D : Any>  {
    inner class Ainner<DD : D?> {
        fun innerFun() {}
    }

    fun test(w:Ainner<*>) {
        w.<!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>innerFun<!>()
    }
}
