// "Create extension function '!u00A0'" "true"
// ERROR: infix modifier is required on '!u00A0' in ''

fun test() {
    val t: Int = 1 <caret>`!u00A0` 2
}