// !API_VERSION: 1.3
// !USE_EXPERIMENTAL: api.ExperimentalAPI
// MODULE: api
// FILE: api.kt

package api

@Experimental(Experimental.Level.ERROR, [Experimental.Impact.COMPILATION])
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
