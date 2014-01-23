// "Import" "true"
// ERROR: Unresolved reference: /

package h

import util.div

trait H

fun f(h: H) {
    h / 3
}
