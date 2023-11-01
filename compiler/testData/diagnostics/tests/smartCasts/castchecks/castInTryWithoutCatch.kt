// ISSUE: KT-56744

fun castInTry(s: Any) {
    try {
        s as String // Potential cast exception
    } finally {
        s.<!UNRESOLVED_REFERENCE!>length<!> // Shouldn't be resolved
    }
    s.<!UNRESOLVED_REFERENCE!>length<!> // Should be smartcast
}

fun castInTryAndFinally(s: Any) {
    try {
        s as String // Potential cast exception
    } finally {
        s as String // Potential cast exception
    }
    <!DEBUG_INFO_SMARTCAST!>s<!>.length // Should be smartcast
}
