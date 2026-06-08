// LL_FIR_DIVERGENCE
// `OTHER_ERROR_WITH_REASON` isn't reported by LL runners.
// LL_FIR_DIVERGENCE
// RUN_PIPELINE_TILL: FRONTEND
class NumColl<T : Collection<Number>>
typealias NumList<T2> = NumColl<List<T2>>
typealias AliasOfNumList<A3> = NumList<A3>

val falseUpperBoundViolation = AliasOfNumList<Int>() // Shouldn't be error
val missedUpperBoundViolation = <!UPPER_BOUND_VIOLATED_IN_TYPEALIAS_EXPANSION_DEPRECATION_WARNING!>NumList<Any>()<!>  // Should be error

/* GENERATED_FIR_TAGS: classDeclaration, nullableType, propertyDeclaration, typeAliasDeclaration,
typeAliasDeclarationWithTypeParameter, typeConstraint, typeParameter */
