// RUN_PIPELINE_TILL: BACKEND
interface Base {
    fun test() = "OK"
}

open class Base2 : Base

class Delegate : Base

fun box(): String {
    object : Base2(), Base by Delegate() {

    }

    return "OK"
}

/* GENERATED_FIR_TAGS: anonymousObjectExpression, classDeclaration, functionDeclaration, inheritanceDelegation,
interfaceDeclaration, stringLiteral */
