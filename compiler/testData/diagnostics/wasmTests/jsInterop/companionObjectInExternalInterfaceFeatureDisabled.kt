// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: -WasmAllowCompanionObjectInExternalInterface
// RENDER_DIAGNOSTICS_FULL_TEXT
// ISSUE: KT-87862, KT-76462

external interface WithCompanion {
    companion <!COMPANION_OBJECT_IN_EXTERNAL_INTERFACE!>object<!> {
        val x: String
    }
}

@JsName("Object")
external interface JsNameObject {
    companion <!COMPANION_OBJECT_IN_EXTERNAL_INTERFACE!>object<!>
}

external interface Outer {
    interface Inner {
        companion <!COMPANION_OBJECT_IN_EXTERNAL_INTERFACE!>object<!>
    }
}

external interface WithNamedCompanion {
    companion <!COMPANION_OBJECT_IN_EXTERNAL_INTERFACE!>object Named<!>
}
