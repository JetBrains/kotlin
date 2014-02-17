// "Import" "true"
// ERROR: Unresolved reference: *=

package h

import util.timesAssign

trait H

fun f(h: H) {
    h *= 3
}
