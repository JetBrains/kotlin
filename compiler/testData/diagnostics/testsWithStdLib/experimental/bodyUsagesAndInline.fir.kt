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
    f()
}

val use2 = f()

// FILE: inline-usage.kt

package usage

import api.*

inline fun inlineUse1() {
    f()
}

inline var inlineUse2: Unit
    get() {
        f()
    }
    set(value) {
        f()
    }

var inlineUse3: Unit
    inline get() {
        f()
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
    f()
}

internal inline fun privateInline2() {
    f()
}

private inline var privateInline3: Unit
    get() {
        f()
    }
    set(value) {
        f()
    }

internal class InternalClass {
    inline fun privateInline4() {
        f()
    }
}
