// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-68590

// KT-68590: Inconsistent qualifier resolution for enum entries and static fields of a class

import MyEnum.A

enum class MyEnum {
    A,
}

class Container {
    enum class A {
        X
    }

    fun member() {
        // Ok: resolved to MyEnum.A
        anyExpression(A)
        // Ok: resolved to Container.A.X (inconsistently with Java static field import case)
        anyExpression(A.<!UNRESOLVED_REFERENCE!>X<!>)
    }
}

fun anyExpression(x: Any?) {}

/* GENERATED_FIR_TAGS: classDeclaration, enumDeclaration, enumEntry, functionDeclaration, nestedClass, nullableType */
