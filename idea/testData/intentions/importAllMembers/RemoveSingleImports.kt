// INTENTION_TEXT: "Import members from 'java.util.regex.Pattern'"
// WITH_RUNTIME

import java.util.regex.Pattern
import java.util.regex.Pattern.matches

fun foo() {
    matches("", "")

    val field = <caret>Pattern.CASE_INSENSITIVE

    Pattern.compile("")

    val fieldFqn = java.util.regex.Pattern.CASE_INSENSITIVE
}
