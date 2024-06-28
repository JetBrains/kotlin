// ISSUE: KT-56744

fun castInTry(s: Any) {
    try {
        s as String // Potential cast exception
    } catch (e: Exception) {
        s.<!UNRESOLVED_REFERENCE!>length<!> // shouldn't be resolved
    } finally {
        s.<!UNRESOLVED_REFERENCE!>length<!> // shouldn't be resolved
    }
    s.<!UNRESOLVED_REFERENCE!>length<!> // shouldn't be resolved
}

fun castInTryAndCatch(s: Any) {
    try {
        s as String // Potential cast exception
    } catch (e: Exception) {
        s as String // Potential cast exception
    } finally {
        s.<!UNRESOLVED_REFERENCE!>length<!> // shouldn't be resolved
    }
    s.<!UNRESOLVED_REFERENCE!>length<!> // should be smartcast
}

fun castAtAll(s: Any) {
    try {
        s as String // Potential cast exception
    } catch (e: Exception) {
        s as String // Potential cast exception
    } finally {
        s as String // Potential cast exception
    }
    <!DEBUG_INFO_SMARTCAST!>s<!>.length
}
