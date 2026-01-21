// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-83756
// IGNORE_REVERSED_RESOLVE
// IGNORE_PARTIAL_BODY_ANALYSIS

interface A {
    val a : Any?
}

open class B {
    val a: Any?
        field = 5
}

class C: B(), A {
    fun usage() = this.a
}

/* GENERATED_FIR_TAGS: classDeclaration, explicitBackingField, functionDeclaration, integerLiteral, interfaceDeclaration,
nullableType, propertyDeclaration, thisExpression */
