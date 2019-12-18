// !USE_EXPERIMENTAL: kotlin.RequiresOptIn api.ExperimentalAPI
// MODULE: api
// FILE: api.kt

package api

@RequiresOptIn(level = RequiresOptIn.Level.ERROR)
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
