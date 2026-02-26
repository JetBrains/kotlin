// RUN_PIPELINE_TILL: FRONTEND

class A {
    val b: Any field: Int

    constructor(a: Int) {
        <!VAL_REASSIGNMENT!>b<!> = 2
    }

    init {
        b = 1
    }
}

class B {
    val b: Any field: Int

    init {
        b = 1
    }
    init {
        <!VAL_REASSIGNMENT!>b<!> = 2
    }
}

class C {
    val b: Any field: Int
    val c: Int

    constructor(a: Int) {
        b = 2
    }

    init {
        c = <!UNINITIALIZED_VARIABLE!>b<!>
    }
}

/* GENERATED_FIR_TAGS: assignment, classDeclaration, explicitBackingField, init, integerLiteral, propertyDeclaration,
secondaryConstructor, smartcast */
