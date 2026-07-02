// TARGET_BACKEND: WASM_JS
// FILE: bar.kt
@file:JsModule("./externalFlagsSimulation.mjs")
package bar

external interface Bitmask<T : Bitmask<T>> : JsAny

sealed external interface SymbolFormatFlags : Bitmask<SymbolFormatFlags> {
    companion object {
        val None: SymbolFormatFlags
        val WriteTypeParametersOrArguments: SymbolFormatFlags
        val UseOnlyExternalAliasing: SymbolFormatFlags
        val AllowAnyNodeKind: SymbolFormatFlags
        val UseAliasDefinedOutsideCurrentScope: SymbolFormatFlags
    }
}

sealed external interface GPUBufferUsage : Bitmask<GPUBufferUsage> {
    companion object {
        val MAP_READ: GPUBufferUsage
        val MAP_WRITE: GPUBufferUsage
        val COPY_SRC: GPUBufferUsage
        val COPY_DST: GPUBufferUsage
        val INDEX: GPUBufferUsage
        val VERTEX: GPUBufferUsage
        val UNIFORM: GPUBufferUsage
        val STORAGE: GPUBufferUsage
        val INDIRECT: GPUBufferUsage
        val QUERY_RESOLVE: GPUBufferUsage
    }
}

// FILE: test.kt
import bar.*

fun <T : JsAny> unsafeBitwiseOr(a: T, b: T): T =
    js("a | b")

fun <T : JsAny> unsafeBitwiseAnd(a: T, b: T): T =
    js("a & b")

inline operator fun <T : Bitmask<T>> T.plus(other: T): T =
    unsafeBitwiseOr(this, other)

inline operator fun <T : Bitmask<T>> T.contains(other: T): Boolean =
    unsafeBitwiseAnd(this, other) == other

fun box(): String {
    val flags = SymbolFormatFlags.WriteTypeParametersOrArguments + SymbolFormatFlags.AllowAnyNodeKind
    if (SymbolFormatFlags.WriteTypeParametersOrArguments !in flags) return "fail: missing Write"
    if (SymbolFormatFlags.AllowAnyNodeKind !in flags) return "fail: missing AllowAnyNodeKind"
    if (SymbolFormatFlags.UseOnlyExternalAliasing in flags) return "fail: unexpected UseOnlyExternalAliasing"

    val usage = GPUBufferUsage.INDEX + GPUBufferUsage.STORAGE + GPUBufferUsage.COPY_DST
    if (GPUBufferUsage.INDEX !in usage) return "fail: missing INDEX"
    if (GPUBufferUsage.STORAGE !in usage) return "fail: missing STORAGE"
    if (GPUBufferUsage.COPY_DST !in usage) return "fail: missing COPY_DST"
    if (GPUBufferUsage.VERTEX in usage) return "fail: unexpected VERTEX"

    return "OK"
}
