// "Import" "true"
// ERROR: Unresolved reference: Delegates
// WITH_RUNTIME
package testing

import some.Delegates

fun foo() {
    val d = <caret>Delegates()
}
/* IGNORE_FIR */