// "Import" "true"
// ERROR: Unresolved reference: *=

package h

interface H

fun f(h: H) {
    h <caret>*= 3
}

/* IGNORE_FIR */