// !USE_EXPERIMENTAL: kotlin.RequiresOptIn
// !EXPERIMENTAL: api.ExperimentalAPI
// MODULE: api
// FILE: api.kt

package api

@RequiresOptIn
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
