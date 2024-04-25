// FIR_IDENTICAL
// DIAGNOSTICS: -NOTHING_TO_INLINE -UNUSED_PARAMETER
// OPT_IN: kotlin.RequiresOptIn
// FILE: api.kt

package api

@RequiresOptIn(level = RequiresOptIn.Level.WARNING)
@Retention(AnnotationRetention.BINARY)
annotation class API

@API
fun f() {}

// FILE: usage.kt

package usage

import api.*

fun use1() {
    <!OPT_IN_USAGE!>f<!>()
}

val use2 = <!OPT_IN_USAGE!>f<!>()

// FILE: inline-usage.kt

package usage

import api.*

inline fun inlineUse1() {
    <!OPT_IN_USAGE!>f<!>()
}

inline var inlineUse2: Unit
    get() {
        <!OPT_IN_USAGE!>f<!>()
    }
    set(value) {
        <!OPT_IN_USAGE!>f<!>()
    }

var inlineUse3: Unit
    inline get() {
        <!OPT_IN_USAGE!>f<!>()
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
    <!OPT_IN_USAGE!>f<!>()
}

internal inline fun privateInline2() {
    <!OPT_IN_USAGE!>f<!>()
}

private inline var privateInline3: Unit
    get() {
        <!OPT_IN_USAGE!>f<!>()
    }
    set(value) {
        <!OPT_IN_USAGE!>f<!>()
    }

internal class InternalClass {
    inline fun privateInline4() {
        <!OPT_IN_USAGE!>f<!>()
    }
}
