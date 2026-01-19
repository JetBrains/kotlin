// LANGUAGE: +CollectionLiterals
// RUN_PIPELINE_TILL: FRONTEND

// models following system
//   typeOf(CL) <: K1
//   K1 <: K2
//   K3 <: K2
//   K3 <: ... (maybe several constraints, from parameter types of lambdas)
fun <K1 : K2, K2, K3 : K2> throughDeclared(k: K1, vararg lam: (K3) -> Unit) { }

fun <K1 : K2, K2 : Set<*>, K3: K2> throughDeclaredWithUBOnK2(k: K1, vararg lam: (K3) -> Unit) { }

fun <K> select(vararg k: K) = k[0]
fun <T> materializeFromLambdaInputs(vararg t: (T) -> Unit): T = null!!

fun test() {
    throughDeclared([], { it: Set<Int> -> })
    throughDeclared(<!ARGUMENT_TYPE_MISMATCH!>[""]<!>, { it: Set<Int> -> })

    // ambiguity or MutableSet
    throughDeclared([], { it: MutableSet<Int> -> }, { it: Set<Int> -> })

    // MutableSet
    throughDeclared(<!UNSUPPORTED_COLLECTION_LITERAL_TYPE!>[]<!>, { it: MutableSet<Int> -> }, { it: MutableSet<String> -> })

    throughDeclaredWithUBOnK2([], { it: Set<Int> -> })
    throughDeclaredWithUBOnK2(<!ARGUMENT_TYPE_MISMATCH!>[""]<!>, { it: Set<Int> -> })

    // ambiguity or MutableSet
    throughDeclaredWithUBOnK2([], { it: MutableSet<Int> -> })

    <!CANNOT_INFER_PARAMETER_TYPE!>select<!>(<!CANNOT_INFER_PARAMETER_TYPE!>select<!>(<!UNSUPPORTED_COLLECTION_LITERAL_TYPE!>[]<!>), <!UNRESOLVED_REFERENCE!>materializeFromLambdaInput<!>({ it: Set<Int> -> }))
    <!CANNOT_INFER_PARAMETER_TYPE!>select<!>(<!CANNOT_INFER_PARAMETER_TYPE!>select<!>(<!UNSUPPORTED_COLLECTION_LITERAL_TYPE!>[]<!>), <!UNRESOLVED_REFERENCE!>materializeFromLambdaInput<!>({ it: MutableSet<String> -> }, { it: Set<Int> -> }))
    select(select([], mutableSetOf()), materializeFromLambdaInputs({ it: MutableSet<String> -> }))

    // ambiguity
    select(select([], mutableSetOf()), materializeFromLambdaInputs({ it: Set<String> -> }))

    // ambiguity or MutableSet
    val expected1: MutableSet<String> = select(select([]), materializeFromLambdaInputs({ it: Set<String> -> }))
    val expected2: Set<String> = select(select([]), materializeFromLambdaInputs({ it: MutableSet<String> -> }))
}

/* GENERATED_FIR_TAGS: capturedType, checkNotNullCall, collectionLiteral, functionDeclaration, integerLiteral,
intersectionType, lambdaLiteral, localProperty, nullableType, outProjection, propertyDeclaration, starProjection,
stringLiteral, typeConstraint, typeParameter, vararg */
