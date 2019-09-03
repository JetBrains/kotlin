@file:Suppress("UNUSED_PARAMETER")

package sample

actual data class A(actual val x: Int, actual val y: Double, val t: String)  {
    actual fun commonFun() {}
    fun platformFun() {}

    actual val z: String by lazy { "" }

    operator fun iterator(): Iterator<Int> = null!!
}

fun testDelegate(): String = getCommonA().z