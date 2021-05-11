// "Import" "true"
// ERROR: Unresolved reference: foo

package c

import g.foo

class B

fun test(a: Any) {
    if (a is B) {
        a.<caret>foo()
    }
}
/* IGNORE_FIR */