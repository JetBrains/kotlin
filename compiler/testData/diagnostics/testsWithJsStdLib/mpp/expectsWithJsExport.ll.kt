// LL_FIR_DIVERGENCE
// KT-82245
// LL_FIR_DIVERGENCE

// IGNORE_FIR_DIAGNOSTICS
// RUN_PIPELINE_TILL: BACKEND
// LANGUAGE: +MultiPlatformProjects -AllowExpectDeclarationsInJsExport
// ALLOW_KOTLIN_PACKAGE
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

actual typealias Export = kotlin.js.JsExport

// FILE: js1.kt
import kotlin.js.*

@Export
actual class WithExportOnExpect {
    actual fun foo() {}
    actual val bar = 42
}

@JsExport
actual class WithExportOnActual {
    actual fun foo() {}
    actual val bar = 42
}

@Export
actual class WithExportTypealiasOnActual {
    actual fun foo() {}
    actual val bar = 42
}

// FILE: js2.kt
@file:JsExport
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
