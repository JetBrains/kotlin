// !USE_EXPERIMENTAL: kotlin.Experimental
// !DIAGNOSTICS: -UNUSED_VARIABLE
// FILE: api.kt

package api

@Experimental(Experimental.Level.WARNING)
annotation class ExperimentalAPI

@ExperimentalAPI
fun function(): String = ""

// FILE: usage-propagate.kts

import api.*

@ExperimentalAPI
fun use() {
    function()
}

<!EXPERIMENTAL_API_USAGE!>function<!>()
<!EXPERIMENTAL_API_USAGE!>use<!>()

// FILE: usage-use.kts

@file:UseExperimental(ExperimentalAPI::class)
import api.*

fun use() {
    function()
}

function()
use()

// FILE: usage-none.kts

import api.*

fun use() {
    <!EXPERIMENTAL_API_USAGE!>function<!>()
}

<!EXPERIMENTAL_API_USAGE!>function<!>()
use()
