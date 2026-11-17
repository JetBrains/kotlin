// WITH_STDLIB
// ES_MODULES
// ISSUE: KT-76462
// IGNORE_BACKEND: WASM_JS
// ^^^ KT-76462: the companion object of an external interface evaluates to an empty JS object

// FILE: externalInterfaceCompanionJsModule.mjs
const x = "O";
function f(s) { return s + "K"; }

// A declaration annotated with @JsModule refers to the default export of the module.
export default { x, f };

// FILE: externalInterfaceCompanionJsModule.kt
@JsModule("./externalInterfaceCompanionJsModule.mjs")
@Suppress("COMPANION_OBJECT_IN_EXTERNAL_INTERFACE")
external interface ModuleStatics : JsAny {
    companion object {
        val x: String
        fun f(s: String): String
    }
}

fun box(): String {
    if (ModuleStatics.x != "O") return "Fail 1: ${ModuleStatics.x}"
    return ModuleStatics.f("O")
}
