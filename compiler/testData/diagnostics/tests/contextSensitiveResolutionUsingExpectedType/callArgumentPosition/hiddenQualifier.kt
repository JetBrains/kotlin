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
    takesE(D)
    takesE(HiddenCompanion)
    takesT(D)
    takesT(HiddenCompanion)
}

fun negative() {
    takesInt(<!ARGUMENT_TYPE_MISMATCH, DEPRECATION_ERROR!>D<!>)
    takesInt(<!ARGUMENT_TYPE_MISMATCH, DEPRECATION_ERROR!>HiddenCompanion<!>)
}

/* GENERATED_FIR_TAGS: classDeclaration, companionObject, enumDeclaration, enumEntry, functionDeclaration,
objectDeclaration, stringLiteral, typeConstraint, typeParameter */
