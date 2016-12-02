// WITH_RUNTIME

import Utils.Companion.foo

val list = listOf(1, 2, 3).map(<caret>::foo)

class Utils {
    companion object {
        fun foo(x: Int) = x
    }
}