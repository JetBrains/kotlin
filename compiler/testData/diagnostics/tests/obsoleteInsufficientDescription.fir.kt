// ISSUE: KT-59567
// WITH_STDLIB

class SomeClass() {
    fun someFunction() {

    }

    suspend fun someSuspendingFunction() {

    }
}

fun main() {
    val someObject = SomeClass()
    val someSuspendingFunctionReference = someObject::someSuspendingFunction
    sequence<Int> {
        println("a function: ${someObject::someFunction}")
        println("a suspending function: ${someObject::someSuspendingFunction}")
        println("a suspending function: $someSuspendingFunctionReference")
    }
}
