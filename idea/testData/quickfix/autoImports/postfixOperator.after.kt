// "Import" "true"
// ERROR: Unresolved reference: ++

import util.inc

trait H

fun f(h: H?) {
    var h1 = h
    h1++
}