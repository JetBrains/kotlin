// "Replace with 'matches(input)'" "true"

import kotlin.text.Regex

fun foo(regex: Regex) {
    regex.<caret>containsMatchIn("")
}