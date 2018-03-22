// !API_VERSION: 1.3
// !DIAGNOSTICS: -UNUSED_VARIABLE
// MODULE: api
// FILE: api.kt

package api

@Experimental(Experimental.Level.WARNING, [Experimental.Impact.COMPILATION])
annotation class ExperimentalAPI

@ExperimentalAPI
fun function(): String = ""

// MODULE: usage1(api)
// FILE: usage-propagate.kts

import api.*

@ExperimentalAPI
fun use() {
    function()
}

<!EXPERIMENTAL_API_USAGE!>function<!>()
<!EXPERIMENTAL_API_USAGE!>use<!>()

// MODULE: usage2(api)
// FILE: usage-use.kts

@file:UseExperimental(ExperimentalAPI::class)
import api.*

fun use() {
    function()
}

<!EXPERIMENTAL_API_USAGE!>function<!>()
use()

// MODULE: usage3(api)
// FILE: usage-none.kts

import api.*

fun use() {
    <!EXPERIMENTAL_API_USAGE!>function<!>()
}

<!EXPERIMENTAL_API_USAGE!>function<!>()
use()
