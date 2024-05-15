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
import kotlin.<!UNRESOLVED_IMPORT!>js<!>.*

actual typealias Export = kotlin.<!UNRESOLVED_REFERENCE!>js<!>.JsExport

// FILE: js1.kt
import kotlin.<!UNRESOLVED_IMPORT!>js<!>.*

@Export
actual class WithExportOnExpect {
    actual fun foo() {}
    actual val bar = 42
}

@<!UNRESOLVED_REFERENCE!>JsExport<!>
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
@file:<!UNRESOLVED_REFERENCE!>JsExport<!>
import kotlin.<!UNRESOLVED_IMPORT!>js<!>.*

actual class WithFileExportOnActual {
    actual fun foo() {}
    actual val bar: Int = 42
}

// FILE: js3.kt

actual class WithExportOnExpectFile {
    actual fun foo() {}
    actual val bar: Int = 42
}
