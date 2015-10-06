// "class org.jetbrains.kotlin.idea.quickfix.AutoImportFix" "false"
// ERROR: Unresolved reference: infix
package x

object infix {
    fun invoke() {

    }
}

fun x() {
    "" <caret>infix ""
}