// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-80492
// LANGUAGE: +CollectionLiterals, +ApproximateLocalTypesInPublicDeclarations

open class Outer {
    companion object {
        private operator fun <!RETURN_TYPE_MISMATCH_OF_OPERATOR_OF!>of<!>() = object : Outer() { }
        operator fun of(p1: String) = object : Outer() { }
        operator fun of(vararg ps: String) = run {
            class Child : Outer()
            Child()
        }
    }
}

/* GENERATED_FIR_TAGS: anonymousObjectExpression, classDeclaration, companionObject, functionDeclaration, lambdaLiteral,
localClass, objectDeclaration, operator, vararg */
