// !DIAGNOSTICS: -UNUSED_VARIABLE -UNUSED_PARAMETER

class TC<T, C : Collection<T>>

typealias TCAlias<T, C> = TC<T, C>
typealias TCAliasT<T> = TC<T, <!UPPER_BOUND_VIOLATED!>Any<!>>
typealias TCAliasC<C> = TC<Any, C>
typealias TCAliasT1<T> = TCAlias<T, <!UPPER_BOUND_VIOLATED!>Any<!>>
typealias TCAliasC1<C> = TCAlias<Any, C>

typealias Test1 = TC<Any, <!UPPER_BOUND_VIOLATED!>Any<!>>
typealias Test2 = TC<Any, Collection<Any>>
typealias Test3 = TCAlias<Any, <!UPPER_BOUND_VIOLATED!>Any<!>>
typealias Test4 = TCAlias<Any, Collection<Any>>
typealias Test5 = <!UPPER_BOUND_VIOLATED_IN_TYPEALIAS_EXPANSION!>TCAliasT<Any><!>
typealias Test6 = TCAliasC<<!UPPER_BOUND_VIOLATED!>Any<!>>
typealias Test7 = TCAliasC<Collection<Any>>
typealias Test8 = <!UPPER_BOUND_VIOLATED_IN_TYPEALIAS_EXPANSION!>TCAliasT1<Any><!>
typealias Test9 = TCAliasC1<<!UPPER_BOUND_VIOLATED!>Any<!>>
typealias Test10 = TCAliasC1<Collection<Any>>
