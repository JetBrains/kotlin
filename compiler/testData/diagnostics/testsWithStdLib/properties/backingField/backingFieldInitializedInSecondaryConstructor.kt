// RUN_PIPELINE_TILL: FRONTEND

class A {
    val a: Any field : Int = 1
    val b: Any field: Int

    constructor(c: Int) {
        b = c
        <!VAL_REASSIGNMENT!>a<!> = c
    }
}

class B {
    val x: Any field: Int

    constructor(b: Int) {
        x = 1
    }

    constructor(b: Int, c: Int): this(b) { }
}

class C(val x: Int = 1) {
    val b: Any <!EXPLICIT_FIELD_MUST_BE_INITIALIZED!>field: Int<!>

    constructor(a: String) : this(a.length) {
        b = x
    }
}

/* GENERATED_FIR_TAGS: assignment, classDeclaration, explicitBackingField, integerLiteral, primaryConstructor,
propertyDeclaration, secondaryConstructor */
