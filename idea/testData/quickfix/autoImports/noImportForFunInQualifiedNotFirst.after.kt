// "class org.jetbrains.kotlin.idea.quickfix.AutoImportFix" "false"
// ERROR: Unresolved reference: externalFun

package testing

fun some() {
  testing.<caret>externalFun()
}