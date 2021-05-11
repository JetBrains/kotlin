// "Import" "true"
// ERROR: Unresolved reference: foo

package h

interface H

fun f(h: H) {
    h <caret>foo h
}
/* IGNORE_FIR */