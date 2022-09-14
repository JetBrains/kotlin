class NumColl<T : Collection<Number>>
typealias NumList<T2> = NumColl<List<T2>>
typealias AliasOfNumList<A3> = NumList<A3>

val falseUpperBoundViolation = AliasOfNumList<<!UPPER_BOUND_VIOLATED!>Int<!>>() // Shouldn't be error
val missedUpperBoundViolation = NumList<Any>()  // Should be error

class Base<T : List<CharSequence>>
typealias Alias<T> = Base<List<T>>
val a = Alias<Any>() // Also should be error