// !USE_EXPERIMENTAL: kotlin.Experimental
// FILE: api.kt

package api

@Experimental(Experimental.Level.WARNING)
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
annotation class ExperimentalAPI

@ExperimentalAPI
class C {
    class D {
        class E {
            class F
        }
    }
}

// FILE: usage-propagate.kt

package usage1

import api.*

@ExperimentalAPI
fun use1() {
    C.D.E.F()
}

@ExperimentalAPI
fun use2(f: C.D.E.F) = f.hashCode()

// FILE: usage-use.kt

package usage2

import api.*

@UseExperimental(ExperimentalAPI::class)
fun use1() {
    C.D.E.F()
}

@UseExperimental(ExperimentalAPI::class)
fun use2(f: C.D.E.F) = f.hashCode()

// FILE: usage-none.kt

package usage3

import api.*

fun use1() {
    C.D.E.F()
}

fun use2(f: C.D.E.F) = f.hashCode()
