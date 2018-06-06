// !USE_EXPERIMENTAL: kotlin.Experimental
// !EXPERIMENTAL: api.ExperimentalAPI
// MODULE: api
// FILE: api.kt

package api

@Experimental
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
