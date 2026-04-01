// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
// DIAGNOSTICS: -ERROR_SUPPRESSION
// FILE: Function1.kt
@file:Suppress("OPT_IN_USAGE")
package foo.bar.baz

import kotlin.wasm.*

<!WASM_EXPORT_CLASH!>@WasmExport fun test() = 1<!>

// FILE: Function2.kt
@file:Suppress("OPT_IN_USAGE")

import kotlin.wasm.*

<!WASM_EXPORT_CLASH!>@WasmExport fun foo() = 1<!>

// FILE: Property1.kt
@file:Suppress("OPT_IN_USAGE")
package foo.bar.baz

import kotlin.wasm.*

<!WASM_EXPORT_CLASH!>@WasmExport("test") fun bar() = 2<!>

// FILE: Property2.kt
@file:Suppress("OPT_IN_USAGE")
package foo

import kotlin.wasm.*

@WasmExport fun bar() = 2

// FILE: Property3.kt
@file:Suppress("OPT_IN_USAGE")

import kotlin.wasm.*

<!WASM_EXPORT_CLASH!>@WasmExport("foo") fun bar() = 3<!>

// FILE: Package1.kt
@file:Suppress("OPT_IN_USAGE")
package foo.bar.baz.test

import kotlin.wasm.*

@WasmExport fun foo1() = 1

// FILE: Package2.kt
@file:Suppress("OPT_IN_USAGE")
package foo.bar.baz.test.a.b.c

import kotlin.wasm.*

@WasmExport fun foo2() = 1
