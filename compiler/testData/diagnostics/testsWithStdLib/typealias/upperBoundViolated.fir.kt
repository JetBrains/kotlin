// RUN_PIPELINE_TILL: FRONTEND
class NumColl<T : Collection<Number>>
typealias NumList<T2> = NumColl<List<T2>>
typealias AliasOfNumList<A3> = NumList<A3>

val falseUpperBoundViolation = AliasOfNumList<Int>() // Shouldn't be error
val missedUpperBoundViolation = NumList<<!UPPER_BOUND_VIOLATED!>Any<!>>()  // Should be error

/* GENERATED_FIR_TAGS: classDeclaration, nullableType, propertyDeclaration, typeAliasDeclaration,
typeAliasDeclarationWithTypeParameter, typeConstraint, typeParameter */
