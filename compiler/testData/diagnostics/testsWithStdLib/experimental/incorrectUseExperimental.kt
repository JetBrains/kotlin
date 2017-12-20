// !API_VERSION: 1.3
// FILE: api.kt

package api

@Experimental(Experimental.Level.WARNING, [Experimental.Impact.RUNTIME])
@Target(AnnotationTarget.PROPERTY)
annotation class BinaryExperimental

@BinaryExperimental
val x = ""

// FILE: usage.kt

import api.*

<!USE_EXPERIMENTAL_WITHOUT_ARGUMENTS!>@UseExperimental<!>
fun use1(): String {
    return <!EXPERIMENTAL_API_USAGE!>x<!>
}

<!USE_EXPERIMENTAL_ARGUMENT_HAS_NON_COMPILATION_IMPACT!>@UseExperimental(BinaryExperimental::class)<!>
fun use2(): String {
    return <!EXPERIMENTAL_API_USAGE!>x<!>
}

<!USE_EXPERIMENTAL_ARGUMENT_IS_NOT_MARKER!>@UseExperimental(UseExperimental::class)<!>
fun use3(): String {
    return <!EXPERIMENTAL_API_USAGE!>x<!>
}
