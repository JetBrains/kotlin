// FIR_IDENTICAL
// RUN_PIPELINE_TILL: FRONTEND
// DIAGNOSTICS: -UNUSED_VARIABLE -UNUSED_PARAMETER -TOPLEVEL_TYPEALIASES_ONLY

class Outer<TO> {
    <!WRONG_MODIFIER_TARGET!>inner<!> typealias LTO = List<TO>

    class Nested<TN> {
        <!WRONG_MODIFIER_TARGET!>inner<!> typealias LTO = List<<!UNRESOLVED_REFERENCE!>TO<!>>
        <!WRONG_MODIFIER_TARGET!>inner<!> typealias LTN = List<TN>

        inner class Inner<TI> {
            <!WRONG_MODIFIER_TARGET!>inner<!> typealias LTO = List<<!UNRESOLVED_REFERENCE!>TO<!>>
            <!WRONG_MODIFIER_TARGET!>inner<!> typealias LTN = List<TN>
            <!WRONG_MODIFIER_TARGET!>inner<!> typealias LTI = List<TI>
        }
    }
}

fun <TF> foo() {
    class Local<TL> {
        <!WRONG_MODIFIER_TARGET!>inner<!> typealias LTF = List<TF>
        <!WRONG_MODIFIER_TARGET!>inner<!> typealias LTL = List<TL>
    }

    fun <TLF> localfun() =
            object {
                <!WRONG_MODIFIER_TARGET!>inner<!> typealias LTF = List<TF>
                <!WRONG_MODIFIER_TARGET!>inner<!> typealias LTLF = List<TLF>
            }
}
