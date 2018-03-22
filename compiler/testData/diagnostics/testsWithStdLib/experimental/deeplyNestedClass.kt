// !API_VERSION: 1.3
// MODULE: api
// FILE: api.kt

package api

@Experimental(Experimental.Level.WARNING, [Experimental.Impact.COMPILATION])
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

// MODULE: usage1(api)
// FILE: usage-propagate.kt

package usage1

import api.*

@ExperimentalAPI
fun use1() {
    C.D.E.F()
}

@ExperimentalAPI
fun use2(f: C.D.E.F) = f.hashCode()

// MODULE: usage2(api)
// FILE: usage-use.kt

package usage2

import api.*

@UseExperimental(ExperimentalAPI::class)
fun use1() {
    C.D.E.F()
}

@UseExperimental(ExperimentalAPI::class)
fun use2(f: <!EXPERIMENTAL_API_USAGE!>C<!>.<!EXPERIMENTAL_API_USAGE!>D<!>.<!EXPERIMENTAL_API_USAGE!>E<!>.<!EXPERIMENTAL_API_USAGE!>F<!>) = f.hashCode()

// MODULE: usage3(api)
// FILE: usage-none.kt

package usage3

import api.*

fun use1() {
    <!EXPERIMENTAL_API_USAGE!>C<!>.<!EXPERIMENTAL_API_USAGE!>D<!>.<!EXPERIMENTAL_API_USAGE!>E<!>.<!EXPERIMENTAL_API_USAGE!>F<!>()
}

fun use2(f: <!EXPERIMENTAL_API_USAGE!>C<!>.<!EXPERIMENTAL_API_USAGE!>D<!>.<!EXPERIMENTAL_API_USAGE!>E<!>.<!EXPERIMENTAL_API_USAGE!>F<!>) = f.<!EXPERIMENTAL_API_USAGE!>hashCode<!>()
