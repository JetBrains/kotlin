// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-88818
// LANGUAGE_FEATURE_TOGGLED: EliminateSecondKindIncorporation

interface Out<out E>

fun <S : Out<T>, T : CharSequence> foo(x: (S) -> Unit) {}

fun bar() {
    foo { x: Out<Any> -> }
}

/* GENERATED_FIR_TAGS: functionDeclaration, functionalType, interfaceDeclaration, lambdaLiteral, nullableType, out,
typeConstraint, typeParameter */
