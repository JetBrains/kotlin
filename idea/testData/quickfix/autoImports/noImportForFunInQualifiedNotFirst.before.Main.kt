// "class org.jetbrains.kotlin.idea.quickfix.AutoImportFix" "false"
// ACTION: Rename reference
// ERROR: Unresolved reference: externalFun

package testing

fun some() {
  testing.<caret>externalFun()
}