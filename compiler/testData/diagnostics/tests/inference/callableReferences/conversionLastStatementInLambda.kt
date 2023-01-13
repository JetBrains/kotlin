// SKIP_TXT
// ISSUE: KT-55729

fun main(b: Boolean) {
    callWithLambda {
        // The only relevant case for KT-55729, Unit conversion should work, but doesn't in K1 1.8.0
        // For K2, it still doesn't work (see KT-55936)
        ::test1
    }

    callWithLambda {
        // Unit conversion should work (for K2 see KT-55936)
        if (b) ::test1 else ::test2
    }

    callWithLambda {
        // That hasn't been working ever in K1 nor K2
        if (b) <!TYPE_MISMATCH!>{
            <!TYPE_MISMATCH!>::<!TYPE_MISMATCH!>test1<!><!>
        }<!> else <!TYPE_MISMATCH!>{
            <!TYPE_MISMATCH!>::<!TYPE_MISMATCH!>test2<!><!>
        }<!>
    }

    callWithLambda {
        // That hasn't been working ever in K1 nor K2
        (<!TYPE_MISMATCH, TYPE_MISMATCH!>::<!TYPE_MISMATCH!>test1<!><!>)
    }
}

fun test1(): String = ""
fun test2(): String = ""

fun callWithLambda(action: () -> () -> Unit) {}
