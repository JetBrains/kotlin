// LANGUAGE: +FullValueClasses
// TARGET_BACKEND: JS_IR, JS_IR_ES6
// Checks how instances of a single-field full value class and of an inline value class are passed to JavaScript:
// to an external function, to a function with a `js(...)` body and through `dynamic`.
// An inline class instance is passed unboxed where its type is known, a full value class instance is always an object.

// FILE: main.kt
value class Full(val x: Int)

inline value class Inline(val x: Int)

@JsName("typeOfValue")
external fun typeOfFullInExternal(value: Full): String

@JsName("typeOfValue")
external fun typeOfInlineInExternal(value: Inline): String

@JsName("identity")
external fun identityOfFull(value: Full): Full

@JsName("identity")
external fun identityOfInline(value: Inline): Inline

fun typeOfFullInJsCode(value: Full): String = js("typeof value")

fun typeOfInlineInJsCode(value: Inline): String = js("typeof value")

fun typeOfDynamic(value: dynamic): String = js("typeof value")

fun box(): String {
    val full = Full(1)
    val inline = Inline(2)

    if (typeOfFullInExternal(full) != "object") return "Fail 1: ${typeOfFullInExternal(full)}"
    if (typeOfInlineInExternal(inline) != "number") return "Fail 2: ${typeOfInlineInExternal(inline)}"

    if (typeOfFullInJsCode(full) != "object") return "Fail 3: ${typeOfFullInJsCode(full)}"
    if (typeOfInlineInJsCode(inline) != "number") return "Fail 4: ${typeOfInlineInJsCode(inline)}"

    if (typeOfDynamic(full) != "object") return "Fail 5: ${typeOfDynamic(full)}"
    if (typeOfDynamic(inline) != "object") return "Fail 6: ${typeOfDynamic(inline)}"

    if (identityOfFull(full).x != 1) return "Fail 7"
    if (identityOfInline(inline).x != 2) return "Fail 8"

    val fullAsDynamic: dynamic = full
    val fullFromDynamic: Full = fullAsDynamic
    if (fullFromDynamic.x != 1) return "Fail 9"

    val inlineAsDynamic: dynamic = inline
    val inlineFromDynamic: Inline = inlineAsDynamic
    if (inlineFromDynamic.x != 2) return "Fail 10"

    return "OK"
}

// FILE: lib.js
function typeOfValue(value) { return typeof value; }
function identity(value) { return value; }
