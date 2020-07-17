// IS_APPLICABLE: false
// WITH_RUNTIME
// ERROR: Unresolved reference: unresolved

import java.util.regex.Pattern

fun foo() {
    Pattern.<caret>unresolved()
}
