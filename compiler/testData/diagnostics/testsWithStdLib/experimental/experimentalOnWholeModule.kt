// !API_VERSION: 1.3
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
