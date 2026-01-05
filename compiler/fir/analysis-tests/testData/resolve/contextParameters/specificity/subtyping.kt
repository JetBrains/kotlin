// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +ContextParameters +ExplicitContextArguments

class ResolvedA
class ResolvedB

fun expectA(vararg a: ResolvedA) {}
fun expectB(vararg b: ResolvedB) {}

open class A
open class B : A()
class C : B()


context(ctx: A) fun foo1() { }
context(ctx: B) <!CONTEXTUAL_OVERLOAD_SHADOWED!>fun foo1()<!> { }
context(ctx: C) <!CONTEXTUAL_OVERLOAD_SHADOWED!>fun foo1()<!> { }

context(a: A) fun foo2(b: C) = ResolvedA()
context(b: A) fun foo2(a: B) = ResolvedB()

context(a: A) fun foo3(b: C = C()) = ResolvedA()
context(b: A) fun foo3(a: B = B()) = ResolvedB()


fun test0() {
    foo1(ctx = A())
    foo1(ctx = B())
    foo1(ctx = C())

    <!OVERLOAD_RESOLUTION_AMBIGUITY!>foo2<!>(a = B(), b = C())
    <!OVERLOAD_RESOLUTION_AMBIGUITY!>foo2<!>(a = C(), b = C())
    val b0 = foo2(a = B(), b = B())

    <!OVERLOAD_RESOLUTION_AMBIGUITY!>foo3<!>(a = B(), b = C())
    <!OVERLOAD_RESOLUTION_AMBIGUITY!>foo3<!>(a = C(), b = C())
    val b1 = foo3(a = B(), b = B())
    val a2 = foo3(a = C())
    val b2 = foo3(b = C())

    expectA(a2)
    expectB(b0, b1, b2)
}

context(ctx: A)
fun test1() {
    foo1()
    foo1(ctx = ctx)

    val a0 = foo2(b = C())
    val a1 = foo2(b = <!ARGUMENT_TYPE_MISMATCH!>B()<!>)
    val b2 = foo2(B())
    val a3 = foo2(C())
    val b4 = foo2(a = B())

    val a5 = foo3(b = C())
    val b6 = foo3(b = B())
    val b7 = foo3(B())
    val a8 = foo3(C())
    val b9 = foo3(a = B())
    <!OVERLOAD_RESOLUTION_AMBIGUITY!>foo3<!>()

    expectA(a0, a1, a3, a5, a8)
    expectB(b2, b4, b6, b7, b9)
}

context(ctx: B)
fun test2() {
    <!OVERLOAD_RESOLUTION_AMBIGUITY!>foo1<!>()
    foo1(ctx = ctx)
}

context(ctx: C)
fun test3() {
    <!OVERLOAD_RESOLUTION_AMBIGUITY!>foo1<!>()
    foo1(ctx = ctx)
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, functionDeclarationWithContext, localProperty,
propertyDeclaration, vararg */
