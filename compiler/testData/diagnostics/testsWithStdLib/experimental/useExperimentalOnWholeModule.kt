// !USE_EXPERIMENTAL: kotlin.Experimental api.ExperimentalAPI
// MODULE: api
// FILE: api.kt

package api

@Experimental(Experimental.Level.ERROR)
annotation class ExperimentalAPI

@ExperimentalAPI
fun function(): String = ""

// MODULE: usage(api)
// FILE: usage.kt

package usage

import api.*

fun use() {
    function()
}
