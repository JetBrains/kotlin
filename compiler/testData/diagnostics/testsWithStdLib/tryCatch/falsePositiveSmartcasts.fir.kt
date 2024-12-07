// RUN_PIPELINE_TILL: FRONTEND
// DIAGNOSTICS: -UNUSED_VARIABLE -UNUSED_VALUE -VARIABLE_WITH_REDUNDANT_INITIALIZER
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
        x<!UNSAFE_CALL!>.<!>length // smartcast is unsafe (OOME could happen after `x = null`)
        throw e
    }
    finally {
        // smartcast is unsafe, `x = null` could've happened
        x<!UNSAFE_CALL!>.<!>length
    }
    // smartcast is unsafe, `x = null` could've happened
    x<!UNSAFE_CALL!>.<!>length
}

// With old DFA of try/catch info about unsound smartcasts after try
//  removes only if there is at least one catch branch that not returns Nothing
fun test2() {
    var x: String? = null
    x = ""

    try {
        x = null
    } catch (e: Exception) {
        // smartcast is unsafe, `x = null` could've happened
        x<!UNSAFE_CALL!>.<!>length
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
        t2<!UNSAFE_CALL!>.<!>not() // smartcast is unsafe, t2 is always null
    }
}

fun test4() {
    var t2: Boolean? = true
    if (t2 != null) { // or `t2 is Boolean`
        try {
            t2 = null
        } finally { }
        t2<!UNSAFE_CALL!>.<!>not() // smartcast is unsafe, t2 is always null
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
        s1.length // smartcast is safe, s1 is always ""
        s2<!UNSAFE_CALL!>.<!>length // smartcast is unsafe, s2 can be null
    }
    s1.length // smartcast is safe, s1 is always ""
    s2.length // smartcast is safe, as we can't reach this point with s2 = null
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
        s<!UNSAFE_CALL!>.<!>length // smartcast is unsafe, s is always null
        requireNotNull(s2)
    }
    s<!UNSAFE_CALL!>.<!>length // smartcast is unsafe, s is always null
    s1.length
    s2.length
}
