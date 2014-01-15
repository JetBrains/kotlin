// "Import" "true"
// ERROR: Unresolved reference: foo

package h

import util.foo

trait H

fun f(h: H) {
    h foo h
}