// ISSUE: KT-72884

// FILE: 1.kt
typealias FirstOrder = () -> String
typealias SecondOrder = (FirstOrder) -> String
typealias ThirdOrder = (SecondOrder) -> String

inline fun firstImpl(): String = "OK"
inline fun secondImpl(crossinline first: FirstOrder): String = first()
inline fun thirdImpl(crossinline second: SecondOrder): String = second(::firstImpl)
inline fun fourthImpl(crossinline third: ThirdOrder): String = third(::secondImpl)

// FILE: 2.kt
fun box() = fourthImpl(::thirdImpl)
