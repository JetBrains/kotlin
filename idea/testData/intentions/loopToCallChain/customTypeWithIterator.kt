// IS_APPLICABLE: false
// WITH_RUNTIME
class X {
    operator fun iterator(): Iterator<String>{
        return emptyList<String>().iterator()
    }
}

fun foo(x: X): String? {
    <caret>for (s in x) {
        if (s.isNotBlank()) {
            return s
        }
    }
    return null
}
