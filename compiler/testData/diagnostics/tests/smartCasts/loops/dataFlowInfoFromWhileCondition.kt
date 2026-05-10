// RUN_PIPELINE_TILL: BACKEND
// DUMP_CFG
interface A {
    fun foo(): Boolean
}
interface B : A
interface C : A

fun test() {
    var a: A? = null
    while (a is B || a is C) {
        a.foo()
    }
}

/* GENERATED_FIR_TAGS: disjunctionExpression, functionDeclaration, interfaceDeclaration, isExpression, localProperty,
nullableType, propertyDeclaration, smartcast, whileLoop */
