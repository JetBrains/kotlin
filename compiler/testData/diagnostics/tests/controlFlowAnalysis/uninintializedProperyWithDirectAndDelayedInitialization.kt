// FIR_IDENTICAL
// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-56678

class A {
    val b = <!UNINITIALIZED_VARIABLE!>a<!>
    val a = 1
    val c = a
}

class B {
    val b = <!UNINITIALIZED_VARIABLE!>a<!>
    val a: Int
    val c = <!UNINITIALIZED_VARIABLE!>a<!>
    init {
        a = 1
    }
    val d = a
}

/* GENERATED_FIR_TAGS: assignment, classDeclaration, init, integerLiteral, propertyDeclaration */
