// RUN_PIPELINE_TILL: CODEGEN
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
    val p1: E = D
    val p2: E = HiddenCompanion
}

/* GENERATED_FIR_TAGS: classDeclaration, companionObject, enumDeclaration, enumEntry, functionDeclaration, localProperty,
objectDeclaration, propertyDeclaration, stringLiteral */
