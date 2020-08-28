// INTENTION_TEXT: "Add import for 'java.util.regex.Pattern.matches'"
// WITH_RUNTIME
// ERROR: Unresolved reference: SomethingElse
// ERROR: Unresolved reference: somethingElse
// SKIP_ERRORS_AFTER

import java.util.regex.Pattern

fun foo() {
    Pattern.<caret>matches("", "")
}

fun bar() {
    Pattern.matches("", "")

    java.util.regex.Pattern.matches("", "")

    Pattern.compile("")

    SomethingElse.matches("", "")

    somethingElse.Pattern.matches("", "")
}