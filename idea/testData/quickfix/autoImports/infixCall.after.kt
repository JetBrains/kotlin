// "Import" "true"
// ERROR: Unresolved reference: foo

import util.foo

trait H

fun f(h: H) {
    h foo h
}