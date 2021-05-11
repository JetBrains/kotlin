// "Import" "true"
// ERROR: Unresolved reference: ++

package h

interface H

fun f(h: H?) {
    var h1 = h
    h1<caret>++
}
/* IGNORE_FIR */