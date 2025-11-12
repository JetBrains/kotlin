// LL_FIR_DIVERGENCE
// KT-82245
// LL_FIR_DIVERGENCE

// IGNORE_FIR_DIAGNOSTICS
// RUN_PIPELINE_TILL: KLIB
// LANGUAGE: +MultiPlatformProjects -AllowExpectDeclarationsInJsExport
// ISSUE: KT-64951

// MODULE: m1-common
// FILE: common1.kt

@Target(AnnotationTarget.FILE, AnnotationTarget.CLASS)
expect annotation class Export()

@Export
expect class WithExportOnExpect {
    fun foo()
    val bar: Int
}

expect class WithExportOnActual {
    fun foo()
    val bar: Int
}

expect class WithExportTypealiasOnActual {
    fun foo()
    val bar: Int
}

expect class WithFileExportOnActual {
    fun foo()
    val bar: Int
}

// FILE: common2.kt
@file:Export

expect class WithExportOnExpectFile {
    fun foo()
    val bar: Int
}

// MODULE: m1-js()()(m1-common)

// FILE: annotation.kt
package kotlin.js

@Target(AnnotationTarget.FILE, AnnotationTarget.CLASS)
annotation class JsExport

// FILE: alias.kt
import kotlin.js.*

actual typealias Export = <!OPT_IN_USAGE!>kotlin.js.JsExport<!>

// FILE: js1.kt
import kotlin.js.*

@<!OPT_IN_USAGE!>Export<!>
actual class WithExportOnExpect {
    actual fun foo() {}
    actual val bar = 42
}

@<!OPT_IN_USAGE!>JsExport<!>
actual class WithExportOnActual {
    actual fun foo() {}
    actual val bar = 42
}

@<!OPT_IN_USAGE!>Export<!>
actual class WithExportTypealiasOnActual {
    actual fun foo() {}
    actual val bar = 42
}

// FILE: js2.kt
@file:<!OPT_IN_USAGE!>JsExport<!>
import kotlin.js.*

actual class WithFileExportOnActual {
    actual fun foo() {}
    actual val bar: Int = 42
}

// FILE: js3.kt

actual class WithExportOnExpectFile {
    actual fun foo() {}
    actual val bar: Int = 42
}
