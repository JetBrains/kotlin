// "Import" "true"
// ERROR: Unresolved reference: ++

package h

import util.inc

interface H

fun f(h: H?) {
    var h1 = h
    h1++
}
/* IGNORE_FIR */