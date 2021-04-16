// "Import" "false"
// IGNORE_IRRELEVANT_ACTIONS
// ERROR: Unresolved reference: infix
package x

object infix {
    fun invoke() {

    }
}

fun x() {
    "" <caret>infix ""
}