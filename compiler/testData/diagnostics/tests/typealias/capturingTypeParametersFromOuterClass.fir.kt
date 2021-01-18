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
        typealias LTF = <!UNRESOLVED_REFERENCE!>List<<!UNRESOLVED_REFERENCE!>TF<!>><!>
        typealias LTL = <!UNRESOLVED_REFERENCE!>List<<!UNRESOLVED_REFERENCE!>TL<!>><!>
    }

    fun <TLF> localfun() =
            object {
                typealias LTF = List<<!UNRESOLVED_REFERENCE!>TF<!>>
                typealias LTLF = List<<!UNRESOLVED_REFERENCE!>TLF<!>>
            }
}
