// !API_VERSION: 1.3
// FILE: api.kt

package api

@Experimental(Experimental.Level.WARNING, [Experimental.Impact.COMPILATION])
@Target(AnnotationTarget.FUNCTION)
annotation class CompilationExperimentalAPI

@Experimental(Experimental.Level.WARNING, [Experimental.Impact.RUNTIME])
@Target(AnnotationTarget.FUNCTION)
annotation class RuntimeExperimentalAPI

@CompilationExperimentalAPI
fun compilation() {}

@RuntimeExperimentalAPI
fun runtime() {}

// FILE: usage.kt

@file:UseExperimental(CompilationExperimentalAPI::class)
package usage

import api.*

fun use() {
    compilation()
    <!EXPERIMENTAL_API_USAGE!>runtime<!>()
}

class Use {
    fun use() {
        compilation()
        <!EXPERIMENTAL_API_USAGE!>runtime<!>()
    }
}
