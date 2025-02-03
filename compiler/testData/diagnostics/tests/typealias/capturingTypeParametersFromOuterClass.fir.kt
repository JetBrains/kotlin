// RUN_PIPELINE_TILL: FRONTEND
// DIAGNOSTICS: -UNUSED_VARIABLE -UNUSED_PARAMETER -TOPLEVEL_TYPEALIASES_ONLY -UNSUPPORTED_FEATURE

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
        <!UNSUPPORTED!><!WRONG_MODIFIER_TARGET!>inner<!> typealias LTF = List<TF><!>
        <!UNSUPPORTED!><!WRONG_MODIFIER_TARGET!>inner<!> typealias LTL = List<TL><!>
    }

    fun <TLF> localfun() =
            object {
                <!UNSUPPORTED!><!WRONG_MODIFIER_TARGET!>inner<!> typealias LTF = List<TF><!>
                <!UNSUPPORTED!><!WRONG_MODIFIER_TARGET!>inner<!> typealias LTLF = List<TLF><!>
            }
}
