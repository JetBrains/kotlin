// LANGUAGE: +MultiPlatformProjects
// DIAGNOSTICS: -ERROR_SUPPRESSION
// WITH_STDLIB
// ISSUE: KT-81592
// MODULE: lib-common
// FILE: common.kt
expect fun Foo()

// MODULE: lib-intermediate()()(lib-common)
// FILE: platform.kt
@Suppress("INVISIBLE_REFERENCE")
@kotlin.internal.LowPriorityInOverloadResolution
actual fun Foo() {}

class Foo()

// MODULE: app-common(lib-common, lib-intermediate)
fun test() {
    <!OVERLOAD_RESOLUTION_AMBIGUITY!>Foo<!>()
}
