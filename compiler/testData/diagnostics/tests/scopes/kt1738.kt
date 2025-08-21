// RUN_PIPELINE_TILL: FRONTEND
//KT-1738 Make it possible to define visibility for constructor parameters which become properties

package kt1738

class A(private var i: Int, var j: Int) {
}

fun test(a: A) {
    a.<!INVISIBLE_MEMBER!>i<!>++
    a.j++
}

/* GENERATED_FIR_TAGS: assignment, classDeclaration, functionDeclaration, incrementDecrementExpression,
primaryConstructor, propertyDeclaration */
