// FIR_IDENTICAL
// WITH_STDLIB
// TARGET_BACKEND: JVM_IR

class Box1<T>()
class Box2<T, R>()

typealias OneToOne<A> = Box1<A>
typealias OneToTwo<A> = Box2<A, A>
typealias TwoToTwo<A, B> = Box2<A, B>
typealias TwoToTwoReversed<A, B> = Box2<B, A>
typealias TwoToOne<A, B> = Box1<A>
typealias OneToOneTransitive<A> = TwoToOne<A, A>
typealias TwoToTwoTransitive<A, B> = OneToTwo<A>
typealias TwoToTwoTransitive2<A, B> = OneToTwo<B>

val test1 = OneToOne<Int>()
val test2 = OneToTwo<Int>()
val test3 = TwoToTwo<Int, String>()
val test4 = TwoToTwoReversed<Int, String>()
val test5 = TwoToOne<Int, String>()
val test6 = OneToOneTransitive<Int>()
val test7 = TwoToTwoTransitive<Int, String>()
val test8 = TwoToTwoTransitive2<Int, String>()
