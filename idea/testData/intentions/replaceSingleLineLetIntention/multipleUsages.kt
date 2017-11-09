// IS_APPLICABLE: false
// WITH_RUNTIME

fun foo(s: String) {
    if (s.substring(1).let<caret> { it.startsWith("a") || it[1].isLowerCase() }) {

    }
}