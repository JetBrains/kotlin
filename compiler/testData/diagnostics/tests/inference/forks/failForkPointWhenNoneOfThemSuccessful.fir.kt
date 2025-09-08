// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-75444

interface Data

sealed interface Token {
    class TokenA : Token
    object TokenB : Token
}

sealed interface Type<K : Data> {
    sealed class TypeA<K : Data> : Type<K>
}

sealed interface Base<out A : Type<out K>, out I : Token, K : Data> {
    sealed interface A<out A : Type.TypeA<out K>, out I : Token, K : Data> : Base<A, I, K>
    sealed interface B<out A : Type<out K>, K : Data> : Base<A, Token.TokenB, K>
}

fun <A : Type.TypeA<out K>, K : Data> Base<A, Token.TokenA, K>.foo() {}

fun test_3_2(algorithm: Base.A<*, Token.TokenB, Data>) {
    algorithm <!UNCHECKED_CAST!>as Base.B<Type<out Data>, Data><!>
    algorithm.<!CANNOT_INFER_PARAMETER_TYPE, UNRESOLVED_REFERENCE_WRONG_RECEIVER!>foo<!>() // should be wrong receiver
}

/* GENERATED_FIR_TAGS: asExpression, classDeclaration, funWithExtensionReceiver, functionDeclaration,
interfaceDeclaration, intersectionType, nestedClass, objectDeclaration, out, outProjection, sealed, smartcast,
starProjection, typeConstraint, typeParameter */
