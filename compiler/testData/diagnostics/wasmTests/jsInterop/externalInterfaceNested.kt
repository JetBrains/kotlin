// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +WasmAllowCompanionObjectInExternalInterface
external interface I {
    interface J

    class <!NESTED_CLASS_IN_EXTERNAL_INTERFACE!>C<!>

    <!NESTED_CLASS_IN_EXTERNAL_INTERFACE!>object O<!>

    enum class <!NESTED_CLASS_IN_EXTERNAL_INTERFACE, WRONG_EXTERNAL_DECLARATION!>E<!>

    companion object
}

external interface NamedCompanion {
    companion <!NAMED_COMPANION_IN_EXTERNAL_INTERFACE!>object Named<!>
}
