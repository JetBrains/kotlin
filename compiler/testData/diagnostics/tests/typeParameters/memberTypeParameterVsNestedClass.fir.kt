// ISSUE: KT-63377
// FIR_DUMP

class OuterClass<OuterParam> {
    class OuterParam

    fun <NestedParam : OuterParam> foo(t: NestedParam) {
        val k: OuterParam = t
        val l: OuterParam = <!INITIALIZER_TYPE_MISMATCH, TYPE_MISMATCH!>OuterParam()<!>
    }

    inner class Inner<NestedParam : OuterParam>(t: NestedParam) {
        val k: OuterParam = t
        val l: OuterParam = <!INITIALIZER_TYPE_MISMATCH, TYPE_MISMATCH!>OuterParam()<!>

        init {
            val m: OuterParam = t
            val n: OuterParam = <!INITIALIZER_TYPE_MISMATCH, TYPE_MISMATCH!>OuterParam()<!>
        }
    }
}
