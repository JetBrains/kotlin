// TARGET_BACKEND: WASM

// FILE: 1.mjs

export function add(x, y) { return x + y; }

export function giveMeFive(x) {
    if (x !== 5)
        throw "I expected 5";
}

// FILE: 2.mjs

function sub(x, y) { return x - y; };

export { sub };
export { sub as "(˹˻𔗎𝅳𝅵𝅷𝅹⁽₍❨❪⟮﴾︵﹙（｟󠀨❲❴⟦⟨⟪⟬⦇⦉⦕⸢⸤︗︷︹︻︽︿﹁﹃﹇﹛﹝［｛｢󠁛󠁻«‘“‹❮" }
export { sub as "~!@#\$%^&*()_+\`-={}|[]\\\\:\\\";'<>?,./" }
export { sub as "" }
export { sub as "\n  \r  \t" }
export default sub;

// FILE: 3.mjs

export function provideUByte() { return -1 }

export function provideUShort() { return -1 }

export function provideUInt() { return -1 }

export function provideULong() { return -1n }

// FILE: wasmImport.kt
import kotlin.wasm.WasmImport

@WasmImport("./1.mjs", "add")
external fun addImportRenamed(x: Int, y: Int): Int

@WasmImport("./1.mjs")
external fun giveMeFive(x: Int): Unit  // Test unit return type

@WasmImport("./1.mjs")
external fun add(x: Int, y: Int): Int

@WasmImport("./2.mjs")
external fun sub(x: Float, y: Float): Float

@WasmImport("./2.mjs", "(˹˻𔗎𝅳𝅵𝅷𝅹⁽₍❨❪⟮﴾︵﹙（｟󠀨❲❴⟦⟨⟪⟬⦇⦉⦕⸢⸤︗︷︹︻︽︿﹁﹃﹇﹛﹝［｛｢󠁛󠁻«‘“‹❮")
external fun sub2(x: Float, y: Float): Float

@WasmImport("./2.mjs", "~!@#\$%^&*()_+`-={}|[]\\\\:\\\";'<>?,./")
external fun sub3(x: Float, y: Float): Float

@WasmImport("./2.mjs", "")
external fun sub4(x: Float, y: Float): Float

@WasmImport("./2.mjs", "\n  \r  \t")
external fun sub5(x: Float, y: Float): Float

@WasmImport("./2.mjs", "default")
external fun sub6(x: Float, y: Float): Float

@WasmImport("./3.mjs", "provideUByte")
external fun provideUByte(): UByte

@WasmImport("./3.mjs", "provideUShort")
external fun provideUShort(): UShort

@WasmImport("./3.mjs", "provideUInt")
external fun provideUInt(): UInt

@WasmImport("./3.mjs", "provideULong")
external fun provideULong(): ULong

fun box(): String {
    if (addImportRenamed(5, 6) != 11) return "Fail1"
    if (add(5, 6) != 11) return "Fail1"
    giveMeFive(5)

    if (sub(5f, 6f) != -1f) return "Fail2"
    if (sub2(5f, 6f) != -1f) return "Fail3"
    if (sub3(5f, 6f) != -1f) return "Fail4"
    if (sub4(5f, 6f) != -1f) return "Fail5"
    if (sub5(5f, 6f) != -1f) return "Fail6"
    if (sub6(5f, 6f) != -1f) return "Fail7"

    if (provideUByte() != UByte.MAX_VALUE) return "Fail9"
    if (provideUShort() != UShort.MAX_VALUE) return "Fail10"
    if (provideUInt() != UInt.MAX_VALUE) return "Fail11"
    if (provideULong() != ULong.MAX_VALUE) return "Fail12"

    return "OK"
}