// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: -WasmAllowCompanionObjectInExternalInterface
// ISSUE: KT-87862, KT-76462

@Suppress(<!ERROR_SUPPRESSION!>"NESTED_CLASS_IN_EXTERNAL_INTERFACE"<!>)
external interface SuppressedByOldId {
    object Nested
    companion <!COMPANION_OBJECT_IN_EXTERNAL_INTERFACE!>object<!>
}

@Suppress(<!ERROR_SUPPRESSION!>"COMPANION_OBJECT_IN_EXTERNAL_INTERFACE"<!>)
external interface SuppressedByNewId {
    <!NESTED_CLASS_IN_EXTERNAL_INTERFACE!>object Nested<!>
    companion object
}
