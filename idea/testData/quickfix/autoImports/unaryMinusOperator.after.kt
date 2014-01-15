// "Import" "true"
// ERROR: Unresolved reference: -

package h

import util.minus

trait H

fun f(h: H?) {
    -h
}