// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-86093

@Deprecated(message = "", level = DeprecationLevel.HIDDEN)
object D

class HiddenCompanion {
    @Deprecated(message = "", level = DeprecationLevel.HIDDEN)
    companion object
}

enum class E {
    D, HiddenCompanion
}

fun takesE(e: E) {}
fun <T : E> takesT(t: T) {}
fun takesInt(i: Int) {}

fun test() {
    takesE(<!ARGUMENT_TYPE_MISMATCH, DEPRECATION_ERROR!>D<!>)
    takesE(<!ARGUMENT_TYPE_MISMATCH, DEPRECATION_ERROR!>HiddenCompanion<!>)
    <!CANNOT_INFER_PARAMETER_TYPE!>takesT<!>(<!ARGUMENT_TYPE_MISMATCH, DEPRECATION_ERROR!>D<!>)
    <!CANNOT_INFER_PARAMETER_TYPE!>takesT<!>(<!ARGUMENT_TYPE_MISMATCH, DEPRECATION_ERROR!>HiddenCompanion<!>)
}

fun negative() {
    takesInt(<!ARGUMENT_TYPE_MISMATCH, DEPRECATION_ERROR!>D<!>)
    takesInt(<!ARGUMENT_TYPE_MISMATCH, DEPRECATION_ERROR!>HiddenCompanion<!>)
}

/* GENERATED_FIR_TAGS: classDeclaration, companionObject, enumDeclaration, enumEntry, functionDeclaration,
objectDeclaration, stringLiteral, typeConstraint, typeParameter */
