// TARGET_BACKEND: WASM
// USE_SHARED_OBJECTS
// WASM_FAILS_IN: SM, JSC
// MODULE: main


// FILE: sharedThreads.kt

@file:OptIn(kotlin.concurrent.atomics.ExperimentalAtomicApi::class)

import kotlin.concurrent.atomics.AtomicInt
import kotlin.wasm.WasmExport

public class Counters {
    var simple: Int = 0
    var atomic: AtomicInt = AtomicInt(0)
}

@JsExport
public fun createCounters(): JsReference<Counters> = Counters().toJsReference()

@JsExport
public fun incCounters(cRef: JsReference<Counters>) {
    val c = cRef.get()
    c.simple++
    c.atomic.addAndFetch(1)
}


// FILE: entry.mjs

import {
    createCounters,
    incCounters,
} from "./index.mjs"

const c = createCounters();
incCounters()

if (c.simple !== 1) {
    throw "Fail 1";
}