@file:Suppress("ACTUAL_WITHOUT_EXPECT")

package aliases

actual interface A {
    actual fun commonFun()
    fun platformFun()
}

typealias A2 = A1
typealias A3 = A

actual typealias B = A

typealias B2 = B
typealias B3 = B1

class PlatformInv<T>(val value: T)