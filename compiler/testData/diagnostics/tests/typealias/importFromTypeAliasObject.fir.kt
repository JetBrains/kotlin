// FILE: 1.kt
package bar

typealias HostAlias = Host

object Host {
    fun foo() {}
}

// FILE: 2.kt
import bar.<!TYPEALIAS_AS_CALLABLE_QUALIFIER_IN_IMPORT_ERROR("HostAlias; Host")!>HostAlias<!>.foo

fun test() {
    foo()
}
