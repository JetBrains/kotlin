// !DIAGNOSTICS: -NOTHING_TO_INLINE
// !API_VERSION: 1.3
// MODULE: api
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

// MODULE: usage(api)
// FILE: usage.kt

package usage

import api.*

@ExperimentalCompilationAPI
@ExperimentalLinkageAPI
fun use() {
    compilation()
    linkage()
}

fun indirectUseNoAnnotation() {
    <!EXPERIMENTAL_API_USAGE, EXPERIMENTAL_API_USAGE!>use<!>()
}

@ExperimentalCompilationAPI
fun indirectUseOptInCompilation() {
    <!EXPERIMENTAL_API_USAGE!>use<!>()
}

@ExperimentalLinkageAPI
fun indirectUseOptInLinkage() {
    <!EXPERIMENTAL_API_USAGE!>use<!>()
}

@ExperimentalCompilationAPI
@ExperimentalLinkageAPI
fun indirectUseOptInBoth() {
    use()
}
