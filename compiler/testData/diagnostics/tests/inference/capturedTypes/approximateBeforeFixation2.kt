// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
// WITH_STDLIB
interface A

class B : A
class C : A

fun main(x: List<String>) {
    x.fold(B()) { acc: A, s -> C() }
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, interfaceDeclaration, lambdaLiteral */
