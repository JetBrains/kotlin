interface Inv<T>
interface Out<out T>
interface In<in T>

typealias InvStar = Inv<*>
typealias InvIn = Inv<in Int>
typealias InvOut = Inv<out Int>
typealias InvT<T> = Inv<T>

typealias OutStar = Out<*>
typealias OutOut = Out<out Int>
typealias OutT<T> = Out<T>

typealias InStar = In<*>
typealias InIn = In<in Int>
typealias InT<T> = In<T>

class Test1 : InvStar
class Test2 : InvIn
class Test3 : InvOut
class Test4 : InvT<*>
class Test5 : InvT<InvT<*>>

class Test6 : OutStar
class Test7 : OutOut
class Test8 : OutT<Int>
class Test9 : OutT<out Int>

class Test10 : InStar
class Test11 : InIn
class Test12 : InT<Int>
class Test13 : InT<in Int>