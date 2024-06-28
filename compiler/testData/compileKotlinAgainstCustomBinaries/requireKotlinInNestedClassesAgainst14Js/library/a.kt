@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
package a

import kotlin.internal.RequireKotlin

class Outer {
    @RequireKotlin("2.44")
    class Nested {
        @RequireKotlin("2.88")
        fun f() {}
    }
}
