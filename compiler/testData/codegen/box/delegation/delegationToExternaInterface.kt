// TARGET_BACKEND: JS_IR, JS_IR_ES6
// ^^^ uses `external interface`, `@nativeInvoke` and `definedExternally`, which are JS-only
// SKIP_IR_DESERIALIZATION_CHECKS
// ^^^ KT-88479: `$$delegate_0` of an external companion gains the `external` flag over a KLIB round-trip
// KJS_WITH_FULL_RUNTIME
// KT-40126

// MODULE: lib
// FILE: l.kt
@file:Suppress("EXTERNAL_DELEGATION")

@Suppress("NESTED_CLASS_IN_EXTERNAL_INTERFACE")
external interface MySymbol {
    companion object : MySymbolConstructor by definedExternally
}
external interface MySymbolConstructor {
    @nativeInvoke
    operator fun invoke(description: String = definedExternally): Any
}

// MODULE: main(lib)
// FILE: f.kt

fun foo(ee: MySymbol?) = "OK"

fun box() = foo(null)
