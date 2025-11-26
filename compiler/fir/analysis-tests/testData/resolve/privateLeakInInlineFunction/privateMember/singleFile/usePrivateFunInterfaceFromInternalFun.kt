// RUN_PIPELINE_TILL: FRONTEND
private fun interface I {
    fun foo(): Int
}

@Suppress(<!ERROR_SUPPRESSION!>"NON_PUBLIC_CALL_FROM_PUBLIC_INLINE"<!>, "IR_PRIVATE_TYPE_USED_IN_NON_PRIVATE_INLINE_FUNCTION_ERROR")
<!NOTHING_TO_INLINE!>inline<!> fun publicInlineFun(): Int = (I { 1 }).foo()

@Suppress(<!ERROR_SUPPRESSION!>"PRIVATE_CLASS_MEMBER_FROM_INLINE"<!>, "IR_PRIVATE_TYPE_USED_IN_NON_PRIVATE_INLINE_FUNCTION_ERROR")
internal <!NOTHING_TO_INLINE!>inline<!> fun internalInlineFun(): Int = (<!LESS_VISIBLE_TYPE_IN_INLINE_ACCESSED_SIGNATURE_ERROR!>I<!> { 1 }).<!LESS_VISIBLE_TYPE_IN_INLINE_ACCESSED_SIGNATURE_ERROR!>foo<!>()

fun box(): String {
    var result = 0
    result += publicInlineFun()
    result += internalInlineFun()
    return if (result == 2) "OK" else result.toString()
}

/* GENERATED_FIR_TAGS: additiveExpression, assignment, equalityExpression, funInterface, functionDeclaration,
ifExpression, inline, integerLiteral, interfaceDeclaration, lambdaLiteral, localProperty, propertyDeclaration,
stringLiteral */
