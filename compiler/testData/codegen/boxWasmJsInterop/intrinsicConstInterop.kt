// TARGET_BACKEND: WASM

// MODULE: lib
// FILE: lib.kt
package constants

const val text = " ok ".trim().uppercase()
const val number = 41.inc()

// MODULE: main(lib)
// FILE: intrinsic-constants.mjs
export function checkConstants(text, number) {
    if (text !== "OK" || number !== 42) throw new Error("Unexpected constants from Wasm");
    return "OK";
}

export const api = { answer: () => 42 };

// FILE: imports.kt
@file:JsModule(" ./intrinsic-constants.mjs ".trim())

package imports

@JsName(" checkConstants ".trim())
external fun callWithConstants(text: String, number: Int): String

// FILE: qualified.kt
@file:JsModule(" ./intrinsic-constants.mjs ".trim())
@file:JsQualifier(" api ".trim())

package qualified

external fun answer(): Int

// FILE: main.kt
import constants.text
import constants.number

const val exportedText = text + Char(33)

@JsExport
fun readConstantText(): String = exportedText

@JsExport
fun readConstantNumber(): Int = number

fun box(): String {
    val actual = qualified.answer()
    if (actual != 42) return "Fail JsQualifier: $actual"
    return imports.callWithConstants(text, number)
}

// FILE: entry.mjs
import "./test.mjs";
import { readConstantText, readConstantNumber } from "./index.mjs";

if (readConstantText() !== "OK!") throw new Error("Unexpected exported text from klib");
if (readConstantNumber() !== 42) throw new Error("Unexpected exported number from klib");
