// "Import" "false"
// ACTION: Rename reference
// ACTION: Convert to run
// ACTION: Convert to with
// ERROR: Unresolved reference: externalFun

package testing

fun some() {
  testing.<caret>externalFun()
}