@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
package a

import kotlin.internal.RequireKotlin

class Outer {
    @RequireKotlin("1.44")
    class Nested {
        @RequireKotlin("1.88")
        fun f() {}
    }
}
