// WITH_RUNTIME
// IS_APPLICABLE: false
// IS_APPLICABLE_2: false
fun foo(): String? {
    Loop@
    while (x()) {
        <caret>for (s in list()) {
            if (s.isEmpty()) continue@Loop
            return s
        }
        return null
    }
    return null
}

fun x(): Boolean = false
fun list(): List<String> = listOf()