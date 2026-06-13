// TARGET_BACKEND: WASM
// WITH_STDLIB

// FILE: test.kt

@JsExport
fun concatAndSlice(prefix: String, value: String, suffix: String): String =
    (prefix + value + suffix).substring(prefix.length, prefix.length + value.length)

@JsExport
fun codeUnitAt(value: String, index: Int): Int =
    value[index].code

@JsExport
fun makeFromChars(): String =
    charArrayOf('A', '\u0000', '\uD83D', '\uDE80', 'Z').concatToString()

@JsExport
fun compareAndCopyThroughChars(lhs: String, rhs: String): String {
    if (lhs != rhs) return "not equal"
    if (lhs.compareTo(rhs) != 0) return "not zero"

    val chars = CharArray(lhs.length)
    lhs.toCharArray(chars, 0, 0, lhs.length)
    return chars.concatToString()
}

private fun jsCodeStringRoundTrip(value: String): String =
    js("value")

@JsExport
fun roundTripThroughJsCode(value: String): String =
    jsCodeStringRoundTrip(value)

fun box(): String = "OK"

// FILE: entry.mjs
import { importObject } from "./index.import-object.mjs";
import { box, codeUnitAt, compareAndCopyThroughChars, concatAndSlice, makeFromChars, roundTripThroughJsCode } from "./index.mjs";

function artifactUrl(name) {
    return import.meta.url.substring(0, import.meta.url.lastIndexOf("/") + 1) + name;
}

// js-string builtins are existing in single-module/multi-module mode in stdlib module
// ownerBase should resolve to either test directory (whole-program mode),
// or to stdlib directory (single-module/multi-module)
const indexImportObjectSource = read(artifactUrl("index.import-object.mjs"));

let ownerBase = "./index";
const stdlibImport = indexImportObjectSource.match(/from\s*['"]([^'"]*kotlin-kotlin-stdlib)\.mjs['"]/);
if (stdlibImport != null) {
    ownerBase = stdlibImport[1];
}

const wrapperSource = read(artifactUrl("index.mjs"));
const ownerSource = read(artifactUrl(ownerBase + ".mjs"));
const ownerImportObjectSource = read(artifactUrl(ownerBase + ".import-object.mjs"));
if (!ownerSource.includes("builtins: ['js-string']")) {
    throw "owner index.mjs does not request js-string builtins";
}
if (!wrapperSource.includes("importedStringConstants: \"'\"")) {
    throw "index.mjs does not request imported string constants";
}
if (!ownerImportObjectSource.includes("StringConstantsProxy")) {
    throw "owner import-object does not define StringConstantsProxy";
}
if (!ownerImportObjectSource.includes("'wasm:js-string'")) {
    throw "owner import-object does not import wasm:js-string";
}

const jsStringBuiltins = await import(artifactUrl(ownerBase + ".js-builtins.mjs"));
const ownerImportObject = (await import(artifactUrl(ownerBase + ".import-object.mjs"))).importObject;

const requiredBuiltins = [
    "length",
    "concat",
    "charCodeAt",
    "substring",
    "compare",
    "equals",
    "fromCharCodeArray",
    "intoCharCodeArray",
];

for (const name of requiredBuiltins) {
    if (typeof jsStringBuiltins[name] !== "function") {
        throw `Missing js-string builtin export: ${name}`;
    }
}

if (ownerImportObject["wasm:js-string"] !== jsStringBuiltins) {
    throw "importObject does not wire wasm:js-string to the js-builtins module";
}

const stringConstants = importObject["'"];
if (stringConstants == null) {
    throw "Missing imported string constants proxy";
}

const importedConstant = "A'\u0000B🚀";
if (stringConstants[importedConstant] !== importedConstant) {
    throw "StringConstantsProxy does not return imported string constants";
}

if (box() !== "OK") {
    throw "Kotlin box failed";
}

const edge = "A\u0000🚀e\u0301Z";
if (concatAndSlice("<<", edge, ">>") !== edge) {
    throw "concat/substring builtins path failed";
}

if (codeUnitAt(edge, 2) !== edge.charCodeAt(2)) {
    throw "charCodeAt builtin path failed";
}

if (makeFromChars() !== "A\u0000🚀Z") {
    throw "fromCharCodeArray builtin path failed";
}

if (compareAndCopyThroughChars(edge, edge) !== edge) {
    throw "equals/compare/intoCharCodeArray builtin path failed";
}

if (roundTripThroughJsCode(edge) !== edge) {
    throw "js_code string roundtrip failed";
}
