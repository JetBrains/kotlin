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

fun test() {
    val p1: E = <!DEPRECATION_ERROR, INITIALIZER_TYPE_MISMATCH!>D<!>
    val p2: E = <!DEPRECATION_ERROR, INITIALIZER_TYPE_MISMATCH!>HiddenCompanion<!>
}

/* GENERATED_FIR_TAGS: classDeclaration, companionObject, enumDeclaration, enumEntry, functionDeclaration, localProperty,
objectDeclaration, propertyDeclaration, stringLiteral */
