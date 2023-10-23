// FIR_IDENTICAL
// !OPT_IN: kotlin.RequiresOptIn
// !DIAGNOSTICS: -UNUSED_VARIABLE
// FILE: api.kt

package api

@RequiresOptIn(level = RequiresOptIn.Level.WARNING)
annotation class ExperimentalAPI

@ExperimentalAPI
fun function(): String = ""

// FILE: usage-propagate.kts

import api.*

@ExperimentalAPI
fun use() {
    function()
}

<!OPT_IN_USAGE!>function<!>()
<!OPT_IN_USAGE!>use<!>()

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
    <!OPT_IN_USAGE!>function<!>()
}

<!OPT_IN_USAGE!>function<!>()
use()
