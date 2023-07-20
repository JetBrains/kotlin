fun castInTry(s: Any) {
    try {
        s as String // Potential cast exception
    } finally {
        s.<!UNRESOLVED_REFERENCE!>length<!> // Shouldn't be resolved
    }
    s.length // Should be smartcast
}

fun castInTryAndFinally(s: Any) {
    try {
        s as String // Potential cast exception
    } finally {
        s as String // Potential cast exception
    }
    s.length // Should be smartcast
}
