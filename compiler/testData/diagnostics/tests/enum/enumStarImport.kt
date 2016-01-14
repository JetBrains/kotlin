// FILE: enum.kt

package enum

enum class HappyEnum {
    CASE1,
    CASE2
}

// FILE: user.kt

import enum.HappyEnum
import enum.HappyEnum.*

fun f(e: HappyEnum) {
    <!DEBUG_INFO_IMPLICIT_EXHAUSTIVE!>when (e) {
        CASE1 -> throw UnsupportedOperationException() // unresolved reference
        CASE2 -> throw UnsupportedOperationException() // unresolved references
    }<!>
}