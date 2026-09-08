// TARGET_BACKEND: WASM
// RUN_THIRD_PARTY_OPTIMIZER

// MODULE: lib
// FILE: externals.js

function foo(f) {
    return f(4, 5);
}

// FILE: lib.kt

external fun foo(f: (Int, Int) -> Int): Int

fun runLibClosureCallHelperTest(): String =
    if (foo { x, y -> x + y } != 9) return "foo" else "OK"

// MODULE: main(lib)
// FILE: main.kt

fun box(): String =
    runLibClosureCallHelperTest()

// FILE: entry.mjs
import "./test.mjs"
import * as moduleExports from "./index.mjs"
import { importObject } from './index.import-object.mjs'

function check(list) {
    const allowedList = ['startUnitTests', 'box'];
    if (!list.every(element => allowedList.includes(element))) {
        throw 'Export list has unexpected elements: expected: [' + allowedList + '] but found [' + list + ']';
    }
}

const wasmBuffer = read('index.wasm', 'binary');
const wasmModule = new WebAssembly.Module(wasmBuffer);
const wasmInstance = new WebAssembly.Instance(wasmModule, importObject);
const wasmExportsList = Object.keys(wasmInstance.exports).filter((v) => !/^__(it|vt|fn|rt).+/.test(v));
check(wasmExportsList);

const exportsList = Object.keys(moduleExports).filter((v) => !v.startsWith('__ALL_EXPORTS'));
check(exportsList);
