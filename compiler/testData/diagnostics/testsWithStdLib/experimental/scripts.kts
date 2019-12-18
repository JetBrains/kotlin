// !USE_EXPERIMENTAL: kotlin.RequiresOptIn
// !DIAGNOSTICS: -UNUSED_VARIABLE
// FILE: api.kt

package api

@RequiresOptIn(RequiresOptIn.Level.WARNING)
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

@file:OptIn(ExperimentalAPI::class)
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
