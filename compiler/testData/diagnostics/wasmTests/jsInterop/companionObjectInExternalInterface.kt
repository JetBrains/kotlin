// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +WasmAllowCompanionObjectInExternalInterface
// ISSUE: KT-76462

external interface WithCompanion {
    companion object {
        val x: String
    }
}

@JsName("Object")
external interface JsNameObject {
    companion object
}

external interface Outer {
    interface Inner {
        companion object
    }
}

external interface WithNamedCompanion {
    companion <!NAMED_COMPANION_IN_EXTERNAL_INTERFACE!>object Named<!>
}
