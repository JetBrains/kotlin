// "Import" "true"
// ERROR: Unresolved reference: *=

import util.timesAssign

trait H

fun f(h: H) {
    h *= 3
}
