// TARGET_BACKEND: WASM
// USE_SHARED_OBJECTS
// WASM_FAILS_IN: V8, SM, JSC
// WASM_FAILS_IN_SINGLE_MODULE_MODE
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
public fun getSimpleCounter(cRef: JsReference<Counters>): Int = cRef.get().simple

@JsExport
public fun getAtomicCounter(cRef: JsReference<Counters>): Int = cRef.get().atomic.load()

@JsExport
public fun incCounters(cRef: JsReference<Counters>) {
    val c = cRef.get()
    c.simple++
    c.atomic.addAndFetch(1)
}

// FILE: worker.mjs

import {
    createCounters,
    incCounters,
    getSimpleCounter,
    getAtomicCounter,
} from "./index.mjs"

onmessage = function (event) {
    console.log("Worker received some counters");
    let c = event.data;
    incCounters(c);
    postMessage("Done");
};

// FILE: entry.mjs

import {
    createCounters,
    incCounters,
    getSimpleCounter,
    getAtomicCounter,
} from "./index.mjs"

let c = createCounters();
let kotlinObject = c.kotlinObject;

const worker = new Worker("worker.mjs", { type: "module" });

function callWorker(data) {
    return new Promise((resolve, reject) => {
        worker.onmessage = (event) => resolve(event.data);
        worker.onerror = (err) => reject(err);
        worker.postMessage(data);
    });
}

(async () => {
    const result = await callWorker(kotlinObject);
    console.log("Worker finished with result: ", result);
    if (getSimpleCounter(c) !== 1) {
        throw "Fail 1";
    }

    if (getAtomicCounter(c) !== 1) {
        throw "Fail 2";
    }
})();