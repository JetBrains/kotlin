class Foo<A : Number>
class Bar<B : CharSequence>

class Hr<A, B, C, D>(val a: A, val b: B)

typealias Test<A, B> = Hr<A, B, Foo<A>, Bar<B>>

val test1 = Test(1, "")
val test2 = <!TYPE_INFERENCE_UPPER_BOUND_VIOLATED(Type parameter bound for B in type inferred from type alias expansion for fun <A, B> <init>\(a: A, b: B\): Test<A, B> /* = Hr<A, B, Foo<A>, Bar<B>> */
 is not satisfied: inferred type Int is not a subtype of CharSequence)!>Test<!>(1, 2)


typealias Bas<T> = Hr<T, T, Foo<T>, Bar<T>>

val test3 = <!TYPE_INFERENCE_UPPER_BOUND_VIOLATED(Type parameter bound for B in type inferred from type alias expansion for fun <T> <init>\(a: T, b: T\): Bas<T> /* = Hr<T, T, Foo<T>, Bar<T>> */
 is not satisfied: inferred type Int is not a subtype of CharSequence)!>Bas<!>(1, 1)
