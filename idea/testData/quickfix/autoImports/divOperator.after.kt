// "Import" "true"
// ERROR: Unresolved reference: /

package h

import util.div

interface H

fun f(h: H) {
    h / 3
}
