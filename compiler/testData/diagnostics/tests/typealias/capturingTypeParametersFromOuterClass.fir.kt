// !DIAGNOSTICS: -UNUSED_VARIABLE -UNUSED_PARAMETER -TOPLEVEL_TYPEALIASES_ONLY

class Outer<TO> {
    typealias LTO = List<TO>

    class Nested<TN> {
        typealias LTO = List<TO>
        typealias LTN = List<TN>

        inner class Inner<TI> {
            typealias LTO = List<TO>
            typealias LTN = List<TN>
            typealias LTI = List<TI>
        }
    }
}

fun <TF> foo() {
    class Local<TL> {
        typealias LTF = <!UNRESOLVED_REFERENCE!>List<TF><!>
        typealias LTL = <!UNRESOLVED_REFERENCE!>List<TL><!>
    }

    fun <TLF> localfun() =
            object {
                typealias LTF = <!UNRESOLVED_REFERENCE!>List<TF><!>
                typealias LTLF = <!UNRESOLVED_REFERENCE!>List<TLF><!>
            }
}
