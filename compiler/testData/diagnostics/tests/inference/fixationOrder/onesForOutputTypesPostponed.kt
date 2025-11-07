// FIR_IDENTICAL
// RUN_PIPELINE_TILL: BACKEND
// WITH_STDLIB
interface A

class B : A
class C : A

fun <T, R> myFold(initial: R, t: T, operation: (acc: R, String, T) -> R, b: (T) -> T): R = TODO()

fun main1(x: List<String>) {
    myFold(B(), 1, { acc: A, s, t  ->
        C()
    }, { it })
}

fun main(x: List<String>) {
    x.fold(B()) { acc: A, s -> C() }
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, interfaceDeclaration, lambdaLiteral */
