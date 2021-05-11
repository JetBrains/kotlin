// "Import" "true"
// ERROR: Unresolved reference: foo

package h

import util.foo

interface H

fun f(h: H) {
    h foo h
}
/* IGNORE_FIR */