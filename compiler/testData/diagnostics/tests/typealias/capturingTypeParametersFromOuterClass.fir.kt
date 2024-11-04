// DIAGNOSTICS: -UNUSED_VARIABLE -UNUSED_PARAMETER -TOPLEVEL_TYPEALIASES_ONLY

class Outer<TO> {
    typealias LTO = List<<!UNRESOLVED_REFERENCE!>TO<!>>

    class Nested<TN> {
        typealias LTO = List<<!UNRESOLVED_REFERENCE!>TO<!>>
        typealias LTN = List<<!UNRESOLVED_REFERENCE!>TN<!>>

        inner class Inner<TI> {
            typealias LTO = List<<!UNRESOLVED_REFERENCE!>TO<!>>
            typealias LTN = List<<!UNRESOLVED_REFERENCE!>TN<!>>
            typealias LTI = List<<!UNRESOLVED_REFERENCE!>TI<!>>
        }
    }
}

fun <TF> foo() {
    class Local<TL> {
        typealias LTF = List<TF>
        typealias LTL = List<TL>
    }

    fun <TLF> localfun() =
            object {
                typealias LTF = List<TF>
                typealias LTLF = List<TLF>
            }
}
