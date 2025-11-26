// RUN_PIPELINE_TILL: FRONTEND
private class Private{
    fun foo() = "OK"
}

@Suppress("IR_PRIVATE_TYPE_USED_IN_NON_PRIVATE_INLINE_FUNCTION_ERROR")
internal <!NOTHING_TO_INLINE!>inline<!> fun internalInlineFun(): String {
    @Suppress(<!ERROR_SUPPRESSION!>"PRIVATE_CLASS_MEMBER_FROM_INLINE"<!>)
    return <!LESS_VISIBLE_TYPE_IN_INLINE_ACCESSED_SIGNATURE_ERROR!>Private<!>().<!LESS_VISIBLE_TYPE_IN_INLINE_ACCESSED_SIGNATURE_ERROR!>foo<!>()
}

fun box(): String {
    return internalInlineFun()
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, inline, stringLiteral */
