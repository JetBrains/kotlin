// RUN_PIPELINE_TILL: BACKEND
// LANGUAGE: +InlineClasses
// DIAGNOSTICS: -UNUSED_PARAMETER, -INLINE_CLASS_DEPRECATED

inline class X(val x: Int)
inline class Z(val x: Int)

class TestOk1(val a: Int, val b: Int) {
    constructor(x: X) : this(x.x, 1)
}

class TestErr1<!CONFLICTING_JVM_DECLARATIONS!>(val a: Int)<!> {
    <!CONFLICTING_JVM_DECLARATIONS!>constructor(x: X) : this(x.x)<!>
}

class <!CONFLICTING_JVM_DECLARATIONS!>TestErr2(val a: Int, val b: Int)<!> {
    <!CONFLICTING_JVM_DECLARATIONS!>constructor(x: X) : this(x.x, 1)<!>
    <!CONFLICTING_JVM_DECLARATIONS!>constructor(z: Z) : this(z.x, 2)<!>
}

/* GENERATED_FIR_TAGS: classDeclaration, integerLiteral, primaryConstructor, propertyDeclaration, secondaryConstructor */
