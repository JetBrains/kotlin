// TARGET_BACKEND: WASM

// FILE: wasmImport.kt
import kotlin.wasm.WasmImport

@WasmImport("foo")
external fun inc1(x: Int): Int

@WasmImport("~!@#\$%^&*()_+`-={}|[]\\\\:\\\";'<>?,./", "inc2")
external fun inc2(x: Int): Int

@WasmImport("./bar.mjs", "inc3")
external fun inc3(x: Int): Int

@WasmImport("./bar.mjs", "giveMeNumber")
external fun giveMeNumber(x: Boolean)

@JsExport
fun myBox(): String {
    if (inc1(5) != 6) return "KFail1"
    if (inc2(5) != 6) return "KFail2"
    if (inc3(5) != 6) return "KFail3"

    // Test that booleans are translated as i32
    giveMeNumber(true)
    giveMeNumber(false)

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

let inc = x => x + 1;

let imports = {
    "foo": { inc1 : inc },
    "~!@#\$%^&*()_+\`-={}|[]\\\\:\\\";'<>?,./" : { inc2 : inc },
    "./bar.mjs" : {
        inc3 : inc,
        giveMeNumber(x) {
            if (typeof x !== 'number') {
                throw "Not a number!";
            }
        },
    },
}

let { exports } = await instantiate(imports);

if (exports.getInitialized() !== 100) {
    throw "Fail1"
}

if (exports.myBox() != "OK") {
    throw "Fail2"
}