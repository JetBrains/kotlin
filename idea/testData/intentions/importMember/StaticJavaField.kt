// INTENTION_TEXT: "Add import for 'java.util.regex.Pattern.CASE_INSENSITIVE'"
// WITH_RUNTIME

import java.util.regex.Pattern

fun foo() {
    val v = Pattern.CASE_INSENSITIVE

    Pattern.<caret>CASE_INSENSITIVE
}
