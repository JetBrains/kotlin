// "Import" "true"
// ERROR: Unresolved reference: -

package h

import util.minus

interface H

fun f(h: H?) {
    -h
}