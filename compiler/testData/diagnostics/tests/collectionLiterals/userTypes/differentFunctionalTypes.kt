// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-83160
// LANGUAGE: +CollectionLiterals
// FIR_DUMP

class Diff {
    companion object {
        operator fun of(vararg x: context(Int, String) () -> Unit) = Diff()
        operator fun of(x: (Int, String) -> Unit) = Diff()
    }
}

context(_: Int, _: String)
fun test() {
    // Here, functional form knows to choose contextual overload, but CL form does not.
    // To choose the overload of `of`, we only apply limited number of resolution stages,
    // because we believe that all types in overloads of `of` must be identical. Here,
    // however, there is a difference. Then we choose non-contextual overload by
    // specificity rules (non-vararg > vararg).
    val p: Diff = Diff.of({ })
    val q: Diff = [<!ARGUMENT_TYPE_MISMATCH!>{ }<!>]
}

/* GENERATED_FIR_TAGS: classDeclaration, companionObject, functionDeclaration, functionDeclarationWithContext,
functionalType, lambdaLiteral, localProperty, objectDeclaration, operator, propertyDeclaration, vararg */
