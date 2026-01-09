// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-83588

open class A<T: Any>(x: T) {
    val y: Any
        field: T = x
}

class B : A<String>("OK") {
    fun foo() {
        println(y.<!UNRESOLVED_REFERENCE!>length<!>)
    }
}

interface C {
    val y: Any
}

class D : A<String>("OK"), C {
    fun bar() {
        println(y.<!UNRESOLVED_REFERENCE!>length<!>)
    }
}

fun main() {
    B().foo()
    D().bar()
}

/* GENERATED_FIR_TAGS: classDeclaration, explicitBackingField, functionDeclaration, primaryConstructor,
propertyDeclaration, smartcast, stringLiteral, typeConstraint, typeParameter */
