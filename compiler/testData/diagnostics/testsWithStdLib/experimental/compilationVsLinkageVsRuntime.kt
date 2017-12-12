// !API_VERSION: 1.3
// FILE: api.kt

package api

@Experimental(Experimental.Level.WARNING, [Experimental.Impact.COMPILATION])
annotation class ExperimentalCompilationAPI

@Experimental(Experimental.Level.WARNING, [Experimental.Impact.LINKAGE])
annotation class ExperimentalLinkageAPI

@Experimental(Experimental.Level.WARNING, [Experimental.Impact.RUNTIME])
annotation class ExperimentalRuntimeAPI

@ExperimentalCompilationAPI
fun compilation() {}

@ExperimentalLinkageAPI
fun linkage() {}

@ExperimentalRuntimeAPI
fun runtime() {}

// FILE: usage.kt

package usage1

import api.*

@UseExperimental(ExperimentalCompilationAPI::class)
@ExperimentalLinkageAPI
@ExperimentalRuntimeAPI
fun use() {
    compilation()
    linkage()
    runtime()
}

@ExperimentalLinkageAPI
@ExperimentalRuntimeAPI
fun useUse() {
    use()
}

@ExperimentalCompilationAPI
@ExperimentalLinkageAPI
@ExperimentalRuntimeAPI
fun recursiveUse() {
    compilation()
    linkage()
    runtime()
    recursiveUse()
}

// FILE: usage-no-annotation.txt

package usage2

import api.*

fun use1() {
    usage1.<!EXPERIMENTAL_API_USAGE, EXPERIMENTAL_API_USAGE!>use<!>()
}

@ExperimentalLinkageAPI
fun use2() {
    usage1.<!EXPERIMENTAL_API_USAGE!>use<!>()
}

@ExperimentalRuntimeAPI
fun use3() {
    usage1.<!EXPERIMENTAL_API_USAGE!>use<!>()
}
