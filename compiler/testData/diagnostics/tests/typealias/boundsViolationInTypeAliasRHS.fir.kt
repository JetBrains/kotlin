// !DIAGNOSTICS: -UNUSED_VARIABLE -UNUSED_PARAMETER

class TC<T, C : Collection<T>>

typealias TCAlias<T, C> = TC<T, C>
typealias TCAliasT<T> = TC<T, Any>
typealias TCAliasC<C> = TC<Any, C>
typealias TCAliasT1<T> = TCAlias<T, Any>
typealias TCAliasC1<C> = TCAlias<Any, C>

typealias Test1 = TC<Any, Any>
typealias Test2 = TC<Any, Collection<Any>>
typealias Test3 = TCAlias<Any, Any>
typealias Test4 = TCAlias<Any, Collection<Any>>
typealias Test5 = TCAliasT<Any>
typealias Test6 = TCAliasC<Any>
typealias Test7 = TCAliasC<Collection<Any>>
typealias Test8 = TCAliasT1<Any>
typealias Test9 = TCAliasC1<Any>
typealias Test10 = TCAliasC1<Collection<Any>>
