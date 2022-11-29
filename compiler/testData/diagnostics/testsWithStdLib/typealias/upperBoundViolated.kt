class NumColl<T : Collection<Number>>
typealias NumList<T2> = NumColl<List<T2>>
typealias AliasOfNumList<A3> = NumList<A3>

val falseUpperBoundViolation = AliasOfNumList<<!UPPER_BOUND_VIOLATED("Collection<Number>; Int")!>Int<!>>() // Shouldn't be error
val missedUpperBoundViolation = NumList<<!UPPER_BOUND_VIOLATED_WARNING("Collection<Number>; List<Any>")!>Any<!>>()  // Should be error
