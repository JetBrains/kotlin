// RUN_PIPELINE_TILL: BACKEND
abstract class A {
    abstract protected fun foo(): String
    abstract protected val bar: String
}

interface B {
    fun foo(): String
    val bar: String
}

fun test(x: A) {
    if (x is B) {
        <!DEBUG_INFO_SMARTCAST!>x<!>.foo()
        <!DEBUG_INFO_SMARTCAST!>x<!>.bar
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, ifExpression, interfaceDeclaration, intersectionType,
isExpression, propertyDeclaration, smartcast */
