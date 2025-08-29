// TARGET_BACKEND: WASM
// USE_NEW_EXCEPTION_HANDLING_PROPOSAL
/// MODULE: main
// FILE: main.kt

import kotlin.wasm.WasmExport

@WasmExport
fun runWithChainedException() {
    val inner = IllegalStateException("Inner cause")
    throw RuntimeException("Outer wrapper", inner)
}

fun box() = "OK"

// FILE: entry.mjs
import { runWithChainedException } from "./index.mjs"

let nothrow = false;
try {
    runWithChainedException();
    nothrow = true;
} catch (e) {
    if (!(e instanceof Error)) {
        throw Error("Expected Error");
    }

    if (e.name !== "RuntimeException") {
        throw Error("Wrong e.name");
    }
    if (e.message !== "Outer wrapper") {
        throw Error("Wrong e.message");
    }

    if (!("cause" in e)) {
        throw Error("Expected 'cause'");
    }
    const c = e.cause;

    if (!(c instanceof Error)) {
        throw Error(
            "Expected cause to be Error"
        );
    }
    if (c.name !== "IllegalStateException") {
        throw Error("Wrong cause.name");
    }
    if (c.message !== "Inner cause") {
        throw Error("Wrong cause.message");
    }

    if ("cause" in c && c.cause != null) {
        throw Error("Expected no nested cause");
    }
}

if (nothrow) throw Error("Unexpected successful call");