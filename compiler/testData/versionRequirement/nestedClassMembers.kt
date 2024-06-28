@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
package test

import kotlin.internal.RequireKotlin

class Outer {
    inner class Inner {
        @RequireKotlin("1.3")
        inner class Deep @RequireKotlin("1.3") constructor() {
            @RequireKotlin("1.3")
            fun f() {}

            @RequireKotlin("1.3")
            val x = ""
        }
    }

    class Nested {
        @RequireKotlin("1.3")
        fun g() {}
    }

    @RequireKotlin("1.3")
    companion object
}

@RequireKotlin("1.3")
fun topLevel() {}
