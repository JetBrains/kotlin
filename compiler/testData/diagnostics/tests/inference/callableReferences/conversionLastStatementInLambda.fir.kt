// SKIP_TXT
// ISSUE: KT-55729

fun main(b: Boolean) {
    callWithLambda {
        // The only relevant case for KT-55729, Unit conversion should work, but doesn't in K1 1.8.0
        // For K2, it still doesn't work (see KT-55936)
        ::<!UNRESOLVED_REFERENCE!>test1<!>
    }

    callWithLambda {
        // Unit conversion should work (for K2 see KT-55936)
        <!ARGUMENT_TYPE_MISMATCH!>if (b) ::test1 else ::test2<!>
    }

    callWithLambda {
        // That hasn't been working ever in K1 nor K2
        <!ARGUMENT_TYPE_MISMATCH!>if (b) {
            ::test1
        } else {
            ::test2
        }<!>
    }

    callWithLambda {
        // That hasn't been working ever in K1 nor K2
        (::<!UNRESOLVED_REFERENCE!>test1<!>)
    }
}

fun test1(): String = ""
fun test2(): String = ""

fun callWithLambda(action: () -> () -> Unit) {}
