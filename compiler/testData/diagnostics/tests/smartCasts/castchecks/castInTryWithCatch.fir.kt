fun castInTry(s: Any) {
    try {
        s as String // Potential cast exception
    } catch (e: Exception) {
        s.length // shouldn't be resolved
    } finally {
        s.length // shouldn't be resolved
    }
    s.length // shouldn't be resolved
}

fun castInTryAndCatch(s: Any) {
    try {
        s as String // Potential cast exception
    } catch (e: Exception) {
        s as String // Potential cast exception
    } finally {
        s.length // shouldn't be resolved
    }
    s.length // should be smartcast
}

fun castAtAll(s: Any) {
    try {
        s as String // Potential cast exception
    } catch (e: Exception) {
        s as String // Potential cast exception
    } finally {
        s as String // Potential cast exception
    }
    s.length
}
