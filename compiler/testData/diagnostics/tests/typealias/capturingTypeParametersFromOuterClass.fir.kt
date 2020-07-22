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
        typealias LTF = <!OTHER_ERROR!>List<TF><!>
        typealias LTL = <!OTHER_ERROR!>List<TL><!>
    }

    fun <TLF> localfun() =
            object {
                typealias LTF = <!OTHER_ERROR!>List<TF><!>
                typealias LTLF = <!OTHER_ERROR!>List<TLF><!>
            }
}
