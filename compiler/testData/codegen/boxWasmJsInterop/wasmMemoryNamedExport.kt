// TARGET_BACKEND: WASM

// MODULE: main
// FILE: main.kt

fun box(): String = "OK"

// FILE: entry.mjs
const messageMemory = "Accessing `memory` via `wasmExports` is deprecated. Use `kotlin.wasm.unsafe.wasmMemory` or update dependencies. Read more: https://kotl.in/vr3szr"

const previousConsole = globalThis.console
const previousConsoleError = previousConsole == null ? undefined : previousConsole.error
if (previousConsole == null) {
    globalThis.console = {}
}
let consoleErrorMessage = ""
globalThis.console.error = (x) => {
    consoleErrorMessage = String(x)
}

try {
    const { memory } = await import("./index.mjs")

    if (consoleErrorMessage !== "") {
        throw Error(`Importing memory named export should not report deprecation: ${consoleErrorMessage}`)
    }

    const buffer = memory.buffer
    if (!(buffer instanceof ArrayBuffer)) {
        throw Error("memory named export does not expose WebAssembly.Memory.buffer")
    }

    if (consoleErrorMessage !== messageMemory) {
        throw Error(`Unexpected deprecation message: ${consoleErrorMessage}`)
    }

    consoleErrorMessage = ""
    const buffer2 = memory.buffer
    if (consoleErrorMessage !== "") {
        throw Error(`Second memory.buffer access should not report deprecation: ${consoleErrorMessage}`)
    }
    if (buffer !== buffer2) {
        throw Error("memory.buffer should return the same buffer")
    }
} finally {
    if (previousConsole == null) {
        delete globalThis.console
    } else {
        globalThis.console = previousConsole
        if (previousConsoleError == null) {
            delete globalThis.console.error
        } else {
            globalThis.console.error = previousConsoleError
        }
    }
}
