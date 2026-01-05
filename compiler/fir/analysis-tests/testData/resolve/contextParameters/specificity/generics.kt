// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +ContextParameters +ExplicitContextArguments

class A
class B

fun expectA(vararg a: A) {}
fun expectB(vararg b: B) {}

interface Box<T>

open class Ca
open class Cb : Ca()



// generic vs non-contextual
fun foo0() = A()

context(t: T)
fun <T> foo0() = B()

val foop0 get() = A()

context(t: T)
val <T> foop0 get() = B()

// generic vs non-generic
context(s: String)
fun foo1() = A()

context(t: T)
fun <T> foo1() = B()

context(s: String)
val foop1 get() = A()

context(t: T)
val <T> foop1 get() = B()

// generic vs non-generic with argument
context(s: String)
fun foo2(argS: String) = A()

context(t: T)
fun <T> foo2(argT: T) = B()

// partially generic vs non-generic
context(s: String, i: Int)
fun foo3() = A()

context(t: T, i: Int)
fun <T> foo3() = B()

context(s: String, i: Int)
val foop3 get() = A()

context(t: T, i: Int)
val <T> foop3 get() = B()

context(i: Int)
fun test0() {
    val a0 = foo0() // should be ambiguous?
    val a1 = foop0  // should be ambiguous?
    val b2 = foo1()
    val b3 = foop1
    val b4 = foo2(42)
    val b5 = foo3()
    val b6 = foop3
    expectA(a0, a1)
    expectB(b2, b3, b4, b5, b6)
}

context(s: String)
fun test1() {
    val a0 = foo0() // should be ambiguous?
    val a1 = foop0  // should be ambiguous?
    val a2 = foo1() // should be ambiguous?
    val a3 = foop1  // should be ambiguous?
    val b4 = foo1<String>()
    expectA(a0, a1, a2, a3)
    expectB(b4)
}

context(s: String, i: Int)
fun test2() {
    val a0 = foo0() // should be ambiguous?
    val a1 = foop0  // should be ambiguous?
    val a2 = foo1() // should be ambiguous?
    val a3 = foop1  // should be ambiguous?
    val b4 = foo1<String>()
    val a5 = foo2("")
    val b6 = foo2(<!ARGUMENT_TYPE_MISMATCH!>42<!>) // should be ok?
    val a7 = foo3() // should be ambiguous?
    val a8 = foop3  // should be ambiguous?
    val b9 = foo3<String>()
    expectA(a0, a1, a2, a3, a5, a7, a8)
    expectB(b4, b9)
}


// deep generic vs non-generic
context(_: Box<String>)
fun bar1() = A()

context(_: Box<T>)
fun <T> bar1() = B()

context(_: Box<String>)
val barp1 get() = A()

context(_: Box<T>)
val <T> barp1 get() = B()

// deep generic with arguments
context(_: Box<T>)
fun <T> bar2(element: Int?) = A()

context(_: Box<T>)
fun <T> bar2(vararg elements: Int?) = B()

// deep generic vs non-generic with argument
context(_: Box<String>)
fun bar3(s: String) = A()

context(_: Box<T>)
fun <T> bar3(t: T) = B()

// generic vs bounded generic
context(_: Box<T>)
fun <T> bar4() = A()

context(_: Box<T>)
<!CONTEXTUAL_OVERLOAD_SHADOWED!>fun <T : Cb> bar4()<!> = B()

context(_: Box<T>)
val <T> barp4 get() = A()

context(_: Box<T>)
<!CONTEXTUAL_OVERLOAD_SHADOWED!>val <T : Cb> barp4<!> get() = B()

context(b: Box<String>)
fun test3() {
    val a0 = bar1() // should be ambiguous?
    val a1 = barp1  // should be ambiguous?

    val b2 = bar2()
    val a3 = bar2(1)
    val a4 = bar2(null)
    val b5 = bar2(1, 2)
    val b6 = bar2(1, null)
    bar2(<!ARGUMENT_TYPE_MISMATCH!>true<!>)

    val a7 = bar3("")
    bar3(<!ARGUMENT_TYPE_MISMATCH!>42<!>)

    expectA(a0, a1, a3, a4, a7)
    expectB(b2, b5, b6)
}

context(b: Box<Int>)
fun test4() {
    val b0 = bar1()
    val b1 = barp1

    <!NO_CONTEXT_ARGUMENT!>bar3<!>("")
    val b2 = bar3(42)

    expectB(b0, b1, b2)
}

context(b: Box<Ca>)
fun test5() {
    bar4()
    barp4
}

context(b: Box<Cb>)
fun test6() {
    <!OVERLOAD_RESOLUTION_AMBIGUITY!>bar4<!>()
    <!OVERLOAD_RESOLUTION_AMBIGUITY!>barp4<!>
}

/* GENERATED_FIR_TAGS: functionDeclaration, functionDeclarationWithContext, integerLiteral, interfaceDeclaration,
nullableType, typeParameter, vararg */
