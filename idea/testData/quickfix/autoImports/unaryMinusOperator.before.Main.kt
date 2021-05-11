// "Import" "true"
// ERROR: Unresolved reference: -

package h

interface H

fun f(h: H?) {
    <caret>-h
}
/* IGNORE_FIR */