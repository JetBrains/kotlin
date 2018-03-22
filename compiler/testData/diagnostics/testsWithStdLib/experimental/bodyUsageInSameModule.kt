// !DIAGNOSTICS: -NOTHING_TO_INLINE
// !API_VERSION: 1.3
// FILE: api.kt

package api

@Experimental(Experimental.Level.WARNING, [Experimental.Impact.COMPILATION])
annotation class ExperimentalCompilationAPI

@Experimental(Experimental.Level.WARNING, [Experimental.Impact.LINKAGE])
annotation class ExperimentalLinkageAPI

@ExperimentalCompilationAPI
fun compilation() {}

@ExperimentalLinkageAPI
fun linkage() {}

// FILE: usage.kt

package usage

import api.*

fun use1() {
    compilation()
    linkage()
}

val use2 = compilation()
val use3 = linkage()

// FILE: inline-usage.kt

package usage

import api.*

inline fun inlineUse1() {
    <!EXPERIMENTAL_API_USAGE!>compilation<!>()
    <!EXPERIMENTAL_API_USAGE!>linkage<!>()
}

inline var inlineUse2: Unit
    get() {
        <!EXPERIMENTAL_API_USAGE!>compilation<!>()
        <!EXPERIMENTAL_API_USAGE!>linkage<!>()
    }
    set(value) {
        <!EXPERIMENTAL_API_USAGE!>compilation<!>()
        <!EXPERIMENTAL_API_USAGE!>linkage<!>()
    }

var inlineUse3: Unit
    inline get() {
        <!EXPERIMENTAL_API_USAGE!>compilation<!>()
        <!EXPERIMENTAL_API_USAGE!>linkage<!>()
    }
    @ExperimentalCompilationAPI
    @ExperimentalLinkageAPI
    inline set(value) {
        compilation()
        linkage()
    }

@ExperimentalCompilationAPI
@ExperimentalLinkageAPI
inline fun inlineUse4() {
    compilation()
    linkage()
}

// FILE: private-inline-usage.kt

package usage

import api.*

private inline fun privateInline1() {
    compilation()
    linkage()
}

internal inline fun privateInline2() {
    compilation()
    linkage()
}

private inline var privateInline3: Unit
    get() {
        compilation()
        linkage()
    }
    set(value) {
        compilation()
        linkage()
    }

internal class InternalClass {
    inline fun privateInline4() {
        compilation()
        linkage()
    }
}
