class NumColl<T : Collection<Number>>
typealias NumList<T2> = NumColl<List<T2>>
typealias AliasOfNumList<A3> = NumList<A3>

val falseUpperBoundViolation = AliasOfNumList<Int>() // Shouldn't be error
val missedUpperBoundViolation = <!UPPER_BOUND_VIOLATED!>NumList<Any>()<!>  // Should be error
