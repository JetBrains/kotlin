// !WITH_NEW_INFERENCE
// !LANGUAGE: +NewDataFlowForTryExpressions
// !DIAGNOSTICS: -UNUSED_VARIABLE -UNUSED_VALUE -VARIABLE_WITH_REDUNDANT_INITIALIZER
// SKIP_TXT
// Related issue: KT-28370

class ExcA : Exception()
class ExcB : Exception()

fun test1(s: String?) {
    var t2: Boolean? = true
    if (t2 != null) {
        try {
            t2 = null
        }
        catch (e: Exception) {
            requireNotNull(s)
        }
        t2<!UNSAFE_CALL!>.<!>not()
        s<!UNSAFE_CALL!>.<!>length
    }
}

fun test2(s: String?) {
    var t2: Boolean? = true
    if (t2 != null) {
        try {
            t2 = null
        }
        finally {
            requireNotNull(s)
            t2 = true
        }
        <!DEBUG_INFO_SMARTCAST!>t2<!>.not()
        <!DEBUG_INFO_SMARTCAST!>s<!>.length
    }
}

fun test3() {
    var s: String? = null
    s = ""
    try {

    }
    catch (e: Exception) {
        s = null
        return
    }
    <!DEBUG_INFO_SMARTCAST!>s<!>.length
}

fun test4() {
    var s: String? = null
    s = ""
    try {

    }
    catch (e: ExcA) {
        s = null
        return
    }
    catch (e: ExcB) {

    }
    <!DEBUG_INFO_SMARTCAST!>s<!>.length
}

fun test5(s: String?) {
    try {
        requireNotNull(s)
    }
    catch (e: ExcA) {
        return
    }
    catch (e: ExcB) {

    }
    s<!UNSAFE_CALL!>.<!>length
}

fun test6(s: String?) {
    try {
        requireNotNull(s)
    }
    catch (e: Exception) {
        return
    }
    <!DEBUG_INFO_SMARTCAST!>s<!>.length
}