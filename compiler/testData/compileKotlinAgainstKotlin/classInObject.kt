// FILE: A.kt

package a

object CartRoutes {
    class RemoveOrderItem {
        val result = "OK"
    }
}

// FILE: B.kt

import a.CartRoutes

fun box(): String {
    val r = CartRoutes.RemoveOrderItem()
    return r.result
}
