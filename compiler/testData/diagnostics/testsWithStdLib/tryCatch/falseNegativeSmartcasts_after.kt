// !WITH_NEW_INFERENCE
// !LANGUAGE: +NewDataFlowForTryExpressions
// !DIAGNOSTICS: -UNUSED_VARIABLE -UNUSED_VALUE -VARIABLE_WITH_REDUNDANT_INITIALIZER
// Related issue: KT-28370

fun test1(s1: String?) {
    var s: String? = null
    s = ""
    try {
        s = ""
        requireNotNull(s1)
    }
    catch (e: Exception) {
        return
    }
    finally {
        <!OI;DEBUG_INFO_SMARTCAST!>s<!><!NI;UNSAFE_CALL!>.<!>length
    }
    <!DEBUG_INFO_SMARTCAST!>s<!>.length
}