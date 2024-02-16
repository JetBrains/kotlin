// SKIP_TXT
// ISSUE: KT-55338

fun test_1() {
    var s: String? = null

    for (i in 1..10) {
        if (s == null) {
            s = "hello"
        }
        s.length // smartcast in K1 and K2
        noInlineRun { s.length } // smartcast in K1 and K2
    }
}

fun test_2_1() {
    var s: String? = null

    for (i in 1..10) {
        if (s == null) {
            s = "hello"
        }
        s.length // smartcast in K1 and K2
        noInlineRun { <!SMARTCAST_IMPOSSIBLE!>s<!>.length } // unsafe call in K1 and K2
        s = ""
    }
}

fun test_2_2() {
    var s: String? = null

    for (i in 1..10) {
        if (s == null) {
            s = "hello"
        }
        s.length // smartcast in K1 and K2
        noInlineRun { <!SMARTCAST_IMPOSSIBLE!>s<!>.length } // unsafe call in K1 and K2
        s = null
    }
}

fun test_3_1() {
    var s: String? = null

    for (i in 1..10) {
        if (s == null) {
            s = "hello"
        } else {
            s = null
        }
        s<!UNSAFE_CALL!>.<!>length // unsafe call in K1 and K2
        noInlineRun { s<!UNSAFE_CALL!>.<!>length } // unsafe call in K1 and K2
    }
}

fun test_3_2() {
    var s: String? = null

    for (i in 1..10) {
        if (s == null) {
            s = "hello"
        } else {
            s = "world"
        }
        s.length // smartcast in K1 and K2
        noInlineRun { s.length } // smartcast in K1 and K2
    }
}

fun test_4_1() {
    var s: String? = null

    for (i in 1..10) {
        if (s == null) {
            s = getString()
        } else {
            s = getNullableString()
        }
        s<!UNSAFE_CALL!>.<!>length // unsafe call in K1 and K2
        noInlineRun { s<!UNSAFE_CALL!>.<!>length } // unsafe call in K1 and K2
    }
}

fun test_4_2() {
    var s: String? = null

    for (i in 1..10) {
        if (s == null) {
            s = getString()
        } else {
            s = getString()
        }
        s.length // smartcast in K1 and K2
        noInlineRun { s.length } // smartcast in K1 and K2
    }
}

fun test_5() {
    var s: String? = null

    for (i in 1..10) {
        s = null
        s = getString()
        s.length // smartcast in K1 and K2
        noInlineRun { <!SMARTCAST_IMPOSSIBLE!>s<!>.length } // smartcast in K1, unsafe call in K2
    }
}

fun getNullableString(): String? = null
fun getString(): String = "hello"
fun noInlineRun(block: () -> Unit) {}
