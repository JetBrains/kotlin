// ISSUE: KT-72884

// FILE: 1.kt
typealias FirstOrder = () -> String
typealias SecondOrder = (FirstOrder) -> String

fun firstImpl() = "OK"
fun secondImpl(first: FirstOrder) = first()
inline fun thirdImpl(crossinline second: SecondOrder) = second(::firstImpl)

// FILE: 2.kt
fun box() = thirdImpl(::secondImpl)
