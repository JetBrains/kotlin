// RUN_PIPELINE_TILL: BACKEND
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

/* GENERATED_FIR_TAGS: callableReference, classDeclaration, functionDeclaration, lambdaLiteral, localProperty,
primaryConstructor, propertyDeclaration, stringLiteral, suspend */
