// TARGET_BACKEND: WASM
// IGNORE_BACKEND: WASM_JS, WASM_WASI

// FILE: wasmImport.kt
import kotlin.wasm.WasmImport

@WasmImport("foo")
external fun inc(x: Int): Int

@JsExport
fun myBox(): String {
    if (inc(5) != 6) return "KFail1"
    return "OK"
}

var initialized: Int = 0

@JsExport
fun getInitialized(): Int = initialized

fun main() {
    initialized = 100
}

// FILE: entry.mjs
import { instantiate } from "./index.uninstantiated.mjs";
import * as d2FzbTpqcy1zdHJpbmc from './js-builtins.mjs';

let inc = x => x + 1;

let imports = {
    "foo": { inc },
    "wasm:js-string" : d2FzbTpqcy1zdHJpbmc,
}

let { exports } = await instantiate(imports, /*runInitializer=*/false);
if (exports.getInitialized() !== 0) {
    throw "Fail1"
}
exports._initialize();
if (exports.getInitialized() !== 100) {
    throw "Fail2"
}

if (exports.myBox() != "OK") {
    throw "Fail3"
}