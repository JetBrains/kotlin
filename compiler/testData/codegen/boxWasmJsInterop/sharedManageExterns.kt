// TARGET_BACKEND: WASM
// USE_SHARED_OBJECTS
// WASM_FAILS_IN: SM, JSC
// MODULE: main


// FILE: sharedManageExterns.kt

import kotlin.wasm.internal.ManagedExternref

external class C(var i: Int)

class Holder(@ManagedExternref val c: C?)

val map: MutableMap<Int, Holder> = mutableMapOf()

@JsExport
public fun saveMapping(key: Int, value: C?) {
    map.put(key, Holder(value))
}

@JsExport
public fun getMapping(key: Int): C? = map.get(key)?.c

// FILE: entry.mjs

import {
    saveMapping,
    getMapping,
} from "./index.mjs"

class C {
    constructor(x) {
        this.x = x;
    }
}

saveMapping(1, new C(1))
saveMapping(2, null)
saveMapping(3, new C(3))
if (getMapping(1).x != 1) throw "Fail 1";
if (getMapping(2) != null) throw "Fail 2";
if (getMapping(3).x != 3) throw "Fail 3";
