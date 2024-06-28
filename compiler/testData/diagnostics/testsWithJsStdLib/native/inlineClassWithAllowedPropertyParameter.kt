// FIR_IDENTICAL
// LANGUAGE: +InlineClasses, -JvmInlineValueClasses, +JsExternalPropertyParameters
// DIAGNOSTICS: +ENUM_CLASS_IN_EXTERNAL_DECLARATION_WARNING

external inline class <!WRONG_EXTERNAL_DECLARATION!>C(val a: Int)<!> {
    fun foo()
}

<!WRONG_MODIFIER_TARGET!>inline<!> external enum class <!ENUM_CLASS_IN_EXTERNAL_DECLARATION_WARNING, WRONG_EXTERNAL_DECLARATION!>E<!> {
    A
}
