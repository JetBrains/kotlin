// !DIAGNOSTICS: -NOTHING_TO_INLINE -UNUSED_PARAMETER
// !USE_EXPERIMENTAL: kotlin.RequiresOptIn
// FILE: api.kt

package api

@RequiresOptIn(RequiresOptIn.Level.WARNING)
annotation class API

@API
fun f() {}

// FILE: usage.kt

package usage

import api.*

fun use1() {
    <!EXPERIMENTAL_API_USAGE!>f<!>()
}

val use2 = <!EXPERIMENTAL_API_USAGE!>f<!>()

// FILE: inline-usage.kt

package usage

import api.*

inline fun inlineUse1() {
    <!EXPERIMENTAL_API_USAGE!>f<!>()
}

inline var inlineUse2: Unit
    get() {
        <!EXPERIMENTAL_API_USAGE!>f<!>()
    }
    set(value) {
        <!EXPERIMENTAL_API_USAGE!>f<!>()
    }

var inlineUse3: Unit
    inline get() {
        <!EXPERIMENTAL_API_USAGE!>f<!>()
    }
    @API
    inline set(value) {
        f()
    }

@API
inline fun inlineUse4() {
    f()
}

// FILE: private-inline-usage.kt

package usage

import api.*

private inline fun privateInline1() {
    <!EXPERIMENTAL_API_USAGE!>f<!>()
}

internal inline fun privateInline2() {
    <!EXPERIMENTAL_API_USAGE!>f<!>()
}

private inline var privateInline3: Unit
    get() {
        <!EXPERIMENTAL_API_USAGE!>f<!>()
    }
    set(value) {
        <!EXPERIMENTAL_API_USAGE!>f<!>()
    }

internal class InternalClass {
    inline fun privateInline4() {
        <!EXPERIMENTAL_API_USAGE!>f<!>()
    }
}
