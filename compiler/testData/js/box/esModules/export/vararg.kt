// ES_MODULES
// WITH_STDLIB

// MODULE: vararg
// FILE: lib.kt
@JsExport
fun uintVararg(vararg uints: UInt): String {
    for (u in uints)  {
        if (u == 0u) return "Failed"
    }

    return "OK"
}

@JsExport
fun uint(a: Int): UInt {
    return a.toUInt()
}

// FILE: main.mjs
// ENTRY_ES_MODULE
import { uint, uintVararg } from "./vararg-vararg_v5.mjs"

export function box() {
    return uintVararg([uint(1), uint(2), uint(3)])
}
