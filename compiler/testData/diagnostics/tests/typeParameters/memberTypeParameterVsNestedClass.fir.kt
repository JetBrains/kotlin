// ISSUE: KT-63377
// FIR_DUMP

class OuterClass<OuterParam> {
    class OuterParam

    fun <NestedParam : OuterParam> foo(t: NestedParam) {
        val k: OuterParam = <!INITIALIZER_TYPE_MISMATCH!>t<!>
        val l: OuterParam = OuterParam()
    }

    inner class Inner<NestedParam : OuterParam>(t: NestedParam) {
        val k: OuterParam = t
        val l: OuterParam = <!INITIALIZER_TYPE_MISMATCH, TYPE_MISMATCH!>OuterParam()<!>

        init {
            val m: OuterParam = <!INITIALIZER_TYPE_MISMATCH!>t<!>
            val n: OuterParam = OuterParam()
        }
    }
}
