// RUN_PIPELINE_TILL: BACKEND
interface A
class B: A

val X = B()

fun foo(b: B) {}

fun main(a: A) {
    if (a === X) {
        foo(a)
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, equalityExpression, functionDeclaration, ifExpression, interfaceDeclaration,
propertyDeclaration, smartcast */
