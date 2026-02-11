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
    throughDeclared([""], { it: Set<Int> -> })

    // ambiguity or MutableSet
    throughDeclared([], { it: MutableSet<Int> -> }, { it: Set<Int> -> })

    // MutableSet
    throughDeclared([], { it: MutableSet<Int> -> }, { it: MutableSet<String> -> })

    throughDeclaredWithUBOnK2([], { it: Set<Int> -> })
    throughDeclaredWithUBOnK2([""], { it: Set<Int> -> })

    // ambiguity or MutableSet
    throughDeclaredWithUBOnK2(<!AMBIGUOUS_COLLECTION_LITERAL!>[]<!>, { it: MutableSet<Int> -> })

    <!CANNOT_INFER_PARAMETER_TYPE!>select<!>(<!CANNOT_INFER_PARAMETER_TYPE!>select<!>(<!UNSUPPORTED_COLLECTION_LITERAL_TYPE!>[]<!>), <!UNRESOLVED_REFERENCE!>materializeFromLambdaInput<!>({ it: Set<Int> -> }))
    <!CANNOT_INFER_PARAMETER_TYPE!>select<!>(<!CANNOT_INFER_PARAMETER_TYPE!>select<!>(<!UNSUPPORTED_COLLECTION_LITERAL_TYPE!>[]<!>), <!UNRESOLVED_REFERENCE!>materializeFromLambdaInput<!>({ it: MutableSet<String> -> }, { it: Set<Int> -> }))
    select(select([], mutableSetOf()), materializeFromLambdaInputs({ it: MutableSet<String> -> }))

    // ambiguity
    select(select(<!AMBIGUOUS_COLLECTION_LITERAL!>[]<!>, mutableSetOf()), materializeFromLambdaInputs({ it: Set<String> -> }))

    // ambiguity or MutableSet
    //  the difference is that in the first case all three TVs are subtypes of MutableSet
    //  in the second, TVs of selects are only subtypes of Set & the third TV is also subtype of MutableSet
    val expected1: MutableSet<String> = select(select([]), materializeFromLambdaInputs({ it: Set<String> -> }))
    val expected2: Set<String> = select(select(<!AMBIGUOUS_COLLECTION_LITERAL!>[]<!>), materializeFromLambdaInputs({ it: MutableSet<String> -> }))
}

/* GENERATED_FIR_TAGS: capturedType, checkNotNullCall, collectionLiteral, functionDeclaration, integerLiteral,
intersectionType, lambdaLiteral, localProperty, nullableType, outProjection, propertyDeclaration, starProjection,
stringLiteral, typeConstraint, typeParameter, vararg */
