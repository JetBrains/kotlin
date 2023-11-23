// ISSUE: KT-63377
// FIR_DUMP

class OuterClassWithObject<OuterParam> {
    object OuterParam {
        fun foo() {}
    }

    val k = ::<!UNRESOLVED_REFERENCE!>OuterParam<!>
    val l = <!CALLABLE_REFERENCE_LHS_NOT_A_CLASS!>OuterParam::<!UNRESOLVED_REFERENCE!>foo<!><!>

    fun foo() {
        val k = ::<!UNRESOLVED_REFERENCE!>OuterParam<!>
        val l = <!CALLABLE_REFERENCE_LHS_NOT_A_CLASS!>OuterParam::<!UNRESOLVED_REFERENCE!>foo<!><!>
    }

    inner class Inner<NestedParam : OuterParam>(t: NestedParam) {
        val k = ::<!UNRESOLVED_REFERENCE!>OuterParam<!>
        val l = <!CALLABLE_REFERENCE_LHS_NOT_A_CLASS!>OuterParam::<!UNRESOLVED_REFERENCE!>foo<!><!>

        init {
            val k = ::<!UNRESOLVED_REFERENCE!>OuterParam<!>
            val l = <!CALLABLE_REFERENCE_LHS_NOT_A_CLASS!>OuterParam::<!UNRESOLVED_REFERENCE!>foo<!><!>
        }
    }
}

class OuterClassWithClass<OuterParam> {
    class OuterParam {
        fun foo() {}
    }

    val k = ::OuterParam
    val l = <!CALLABLE_REFERENCE_LHS_NOT_A_CLASS!>OuterParam::<!UNRESOLVED_REFERENCE!>foo<!><!>
    val m = OuterParam()::foo

    fun foo() {
        val k = ::OuterParam
        val l = <!CALLABLE_REFERENCE_LHS_NOT_A_CLASS!>OuterParam::<!UNRESOLVED_REFERENCE!>foo<!><!>
        val m = OuterParam()::foo
    }

    inner class Inner<NestedParam : OuterParam>(t: NestedParam) {
        val k = ::OuterParam
        val l = <!CALLABLE_REFERENCE_LHS_NOT_A_CLASS!>OuterParam::<!UNRESOLVED_REFERENCE!>foo<!><!>
        val m = OuterParam()::foo

        init {
            val k = ::OuterParam
            val l = <!CALLABLE_REFERENCE_LHS_NOT_A_CLASS!>OuterParam::<!UNRESOLVED_REFERENCE!>foo<!><!>
            val m = OuterParam()::foo
        }
    }
}
