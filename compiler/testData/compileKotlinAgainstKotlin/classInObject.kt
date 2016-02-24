// FILE: A.kt

package a

object CartRoutes {
    class RemoveOrderItem()
}

// FILE: B.kt

import a.CartRoutes

fun main(args: Array<String>) {
    val r = CartRoutes.RemoveOrderItem()
}
