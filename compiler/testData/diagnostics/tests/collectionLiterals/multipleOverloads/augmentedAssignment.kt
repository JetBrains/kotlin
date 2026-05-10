// LANGUAGE: +CollectionLiterals
// RUN_PIPELINE_TILL: FRONTEND
// WITH_STDLIB
// RENDER_DIAGNOSTIC_ARGUMENTS

class A {
    operator fun plus(other: List<Int>) = A()
    operator fun plusAssign(other: Set<Int>) { }
}

class B {
    operator fun plus(other: List<Int>) = B()
    operator fun plusAssign(other: Set<String>) { }
}

class C {
    operator fun plus(other: List<Int>) = C()
    operator fun plusAssign(other: Set<Long>) { }
}

class D {
    var a = A()
    var b = B()
    var c = C()
}

fun test1() {
    var a = A()
    var b = B()
    var c = C()

    a <!ASSIGN_OPERATOR_AMBIGUITY("fun plus(other: List<Int>): Afun plusAssign(other: Set<Int>): Unit")!>+=<!> [1, 2, 3]
    b += [1, 2, 3]
    b += ["!"]
    c <!ASSIGN_OPERATOR_AMBIGUITY("fun plus(other: List<Int>): Cfun plusAssign(other: Set<Long>): Unit")!>+=<!> [1, 2, 3]
    c += [1L, 2, 3]
}

fun test2(d: D) {
    d.a <!ASSIGN_OPERATOR_AMBIGUITY("fun plus(other: List<Int>): Afun plusAssign(other: Set<Int>): Unit")!>+=<!> [1, 2, 3]
    d.b += [1, 2, 3]
    d.b += ["!"]
    d.c <!ASSIGN_OPERATOR_AMBIGUITY("fun plus(other: List<Int>): Cfun plusAssign(other: Set<Long>): Unit")!>+=<!> [1, 2, 3]
    d.c += [1, 2L, 3]
}

fun test3(a: Array<A>, b: Array<B>, c: Array<C>) {
    a[0] <!ASSIGN_OPERATOR_AMBIGUITY("fun plusAssign(other: Set<Int>): Unitfun set(index: Int, value: A): Unit")!>+=<!> [1, 2, 3]
    b[0] += [1, 2, 3]
    b[0] += ["!"]
    c[0] <!ASSIGN_OPERATOR_AMBIGUITY("fun plusAssign(other: Set<Long>): Unitfun set(index: Int, value: C): Unit")!>+=<!> [1, 2, 3]
    c[0] += [1, 2, 3L]
}

/* GENERATED_FIR_TAGS: additiveExpression, assignment, classDeclaration, functionDeclaration, integerLiteral,
localProperty, operator, propertyDeclaration */
