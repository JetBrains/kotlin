// "class org.jetbrains.kotlin.idea.quickfix.ImportFix" "false"
// ACTION: Rename reference
// ERROR: Unresolved reference: externalFun

package testing

fun some() {
  testing.<caret>externalFun()
}