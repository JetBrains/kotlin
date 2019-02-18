// !WITH_NEW_INFERENCE
// !LANGUAGE: +NewDataFlowForTryExpressions
// !DIAGNOSTICS: -UNUSED_VARIABLE -UNUSED_VALUE -VARIABLE_WITH_REDUNDANT_INITIALIZER
// SKIP_TXT
// Related issue: KT-28370

class ExcA : Exception()
class ExcB : Exception()

fun test1() {
    var x: String? = null
    x = ""

    try {
        x = null
    } catch (e: Exception) {
        <!OI;DEBUG_INFO_SMARTCAST!>x<!><!NI;UNSAFE_CALL!>.<!>length // smartcast shouldn't be allowed (OOME could happen after `x = null`)
        throw e
    }
    finally {
        // smartcast shouldn't be allowed, `x = null` could've happened
        <!OI;DEBUG_INFO_SMARTCAST!>x<!><!NI;UNSAFE_CALL!>.<!>length
    }
    // smartcast shouldn't be allowed, `x = null` could've happened
    <!OI;DEBUG_INFO_SMARTCAST!>x<!><!NI;UNSAFE_CALL!>.<!>length
}

// With old DFA of try/catch info about unsound smartcasts after try
//  removes only if there is at least one catch branch that not returns Nothing
fun test2() {
    var x: String? = null
    x = ""

    try {
        x = null
    } catch (e: Exception) {
        // BAD
        <!OI;DEBUG_INFO_SMARTCAST!>x<!><!NI;UNSAFE_CALL!>.<!>length
    }
    finally {
        x<!UNSAFE_CALL!>.<!>length
    }
    x<!UNSAFE_CALL!>.<!>length
}

fun test3() {
    var t2: Boolean? = true
    if (t2 != null) { // or `t2 is Boolean`
        try {
            throw Exception()
        } catch (e: Exception) {
            t2 = null
        }
        <!OI;DEBUG_INFO_SMARTCAST!>t2<!><!NI;UNSAFE_CALL!>.<!>not() // wrong smartcast, NPE
    }
}

fun test4() {
    var t2: Boolean? = true
    if (t2 != null) { // or `t2 is Boolean`
        try {
            t2 = null
        } finally { }
        <!OI;DEBUG_INFO_SMARTCAST!>t2<!><!NI;UNSAFE_CALL!>.<!>not() // wrong smartcast, NPE
    }
}

fun test5() {
    var s1: String? = null
    var s2: String? = null
    s1 = ""
    s2 = ""
    try {
        TODO()
    }
    catch (e: ExcA) {
        s1 = ""
    }
    catch (e: ExcB) {
        s2 = null
        return
    }
    finally {
        <!OI;DEBUG_INFO_SMARTCAST!>s1<!><!NI;UNSAFE_CALL!>.<!>length
        <!OI;DEBUG_INFO_SMARTCAST!>s2<!><!NI;UNSAFE_CALL!>.<!>length
    }
    <!OI;DEBUG_INFO_SMARTCAST!>s1<!><!NI;UNSAFE_CALL!>.<!>length
    <!DEBUG_INFO_SMARTCAST!>s2<!>.length
}

fun test6(s1: String?, s2: String?) {
    var s: String? = null
    s = ""
    try {
        s = null
        requireNotNull(s1)
    }
    catch (e: Exception) {
        return
    }
    finally {
        <!OI;DEBUG_INFO_SMARTCAST!>s<!><!NI;UNSAFE_CALL!>.<!>length
        requireNotNull(s2)
    }
    <!OI;DEBUG_INFO_SMARTCAST!>s<!><!NI;UNSAFE_CALL!>.<!>length
    <!NI;DEBUG_INFO_SMARTCAST!>s1<!><!OI;UNSAFE_CALL!>.<!>length
    <!DEBUG_INFO_SMARTCAST!>s2<!>.length
}