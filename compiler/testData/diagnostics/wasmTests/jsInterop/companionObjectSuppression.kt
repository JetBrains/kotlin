// RUN_PIPELINE_TILL: FRONTEND

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
