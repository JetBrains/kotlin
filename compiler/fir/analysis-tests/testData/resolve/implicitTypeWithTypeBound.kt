// FILE: main.kt
fun <T> myRun(action: () -> T): T = action()

fun test(other: TypeWithBoundedGeneric<*>) = myRun { other }

interface SomeType

abstract class TypeWithBoundedGeneric<T : SomeType>
