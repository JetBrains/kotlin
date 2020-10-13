fun castInTry(s: Any) {
    try {
        s as String // Potential cast exception
    } finally {
        s.length // Shouldn't be resolved
    }
    s.length // Shouldn't be resolved
}

fun castInTryAndFinally(s: Any) {
    try {
        s as String // Potential cast exception
    } finally {
        s as String // Potential cast exception
    }
    s.length // Should be smartcast
}
