infix fun <A, B> A.to(other: B) = this

open class A<T>(x: T)

class B : A(<selection>1 to 2</selection>)

// CALL: KtFunctionCall: targetFunction = /to(<receiver>: A, other: B): A