// "Add remaining branches with * import" "true"

package u

import e.OwnEnum
import e.OwnEnum.*
import e.getOwnEnum

fun mainContext() {
    val ownLocal = getOwnEnum()
    when (ownLocal) {
        RED -> TODO()
        GREEN -> TODO()
        BLUE -> TODO()
    }
}