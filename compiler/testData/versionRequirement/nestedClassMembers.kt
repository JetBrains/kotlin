@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
package test

import kotlin.internal.RequireKotlin

class Outer {
    inner class Inner {
        @RequireKotlin("1.1")
        inner class Deep @RequireKotlin("1.1") constructor() {
            @RequireKotlin("1.1")
            fun f() {}

            @RequireKotlin("1.1")
            val x = ""

            suspend fun s() {}
        }
    }

    class Nested {
        @RequireKotlin("1.1")
        fun g() {}
    }

    @RequireKotlin("1.1")
    companion object
}

@RequireKotlin("1.1")
fun topLevel() {}
