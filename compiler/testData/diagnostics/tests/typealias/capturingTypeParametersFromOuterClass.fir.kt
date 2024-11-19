// RUN_PIPELINE_TILL: FRONTEND
// DIAGNOSTICS: -UNUSED_VARIABLE -UNUSED_PARAMETER -TOPLEVEL_TYPEALIASES_ONLY

class Outer<TO> {
    inner typealias LTO = List<<!UNRESOLVED_REFERENCE!>TO<!>>

    class Nested<TN> {
        inner typealias LTO = List<<!UNRESOLVED_REFERENCE!>TO<!>>
        inner typealias LTN = List<<!UNRESOLVED_REFERENCE!>TN<!>>

        inner class Inner<TI> {
            inner typealias LTO = List<<!UNRESOLVED_REFERENCE!>TO<!>>
            inner typealias LTN = List<<!UNRESOLVED_REFERENCE!>TN<!>>
            inner typealias LTI = List<<!UNRESOLVED_REFERENCE!>TI<!>>
        }
    }
}

fun <TF> foo() {
    class Local<TL> {
        inner typealias LTF = List<TF>
        inner typealias LTL = List<TL>
    }

    fun <TLF> localfun() =
            object {
                inner typealias LTF = List<TF>
                inner typealias LTLF = List<TLF>
            }
}
