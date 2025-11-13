// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +ContextParameters +ExplicitContextArguments
// FIR_DUMP

class A
class B

// scenario 1: more contexts
fun foo1_1() { }
context(a: A) fun foo1_1() { }

fun example1() { foo1_1() }
context(a: A) fun example1a() { <!OVERLOAD_RESOLUTION_AMBIGUITY!>foo1_1<!>() }
fun example1b1() { foo1_1(a = A()) }
context(a: A) fun example1b2() { foo1_1(a = A()) }

context(b: B) fun foo1_2() { }
context(b: B, a: A) <!CONTEXTUAL_OVERLOAD_SHADOWED!>fun foo1_2()<!> { }

context(a: A, b: B) fun example1c() { <!OVERLOAD_RESOLUTION_AMBIGUITY!>foo1_2<!>() }
context(b: B) fun example1d1() { foo1_2(a = A()) }
context(a: A, b: B) fun example1d2() { foo1_2(a = A()) }

// scenario 2: context vs. value parameter
context(a: A) fun foo2() { }
fun foo2(a: A) { }

context(a: A) fun example2a() { foo2() }
fun example2b1() { <!OVERLOAD_RESOLUTION_AMBIGUITY!>foo2<!>(a = A()) }
context(a: A) fun example2b2() { <!OVERLOAD_RESOLUTION_AMBIGUITY!>foo2<!>(a = A()) }

// scenario 3: context vs. optional parameter
context(a: A) fun foo3_1() { }
fun foo3_1(a: A = A()) { }

context(a: A) fun example3a() { <!OVERLOAD_RESOLUTION_AMBIGUITY!>foo3_1<!>() }
fun example3b1() { <!OVERLOAD_RESOLUTION_AMBIGUITY!>foo3_1<!>(a = A()) }
context(a: A) fun example3b2() { <!OVERLOAD_RESOLUTION_AMBIGUITY!>foo3_1<!>(a = A()) }

context(b: B, a: A) fun foo3_2() { }
context(b: B) fun foo3_2(a: A = A()) { }

context(a: A, b: B) fun example3c() { <!OVERLOAD_RESOLUTION_AMBIGUITY!>foo3_2<!>() }
context(b: B) fun example3d1() { <!OVERLOAD_RESOLUTION_AMBIGUITY!>foo3_2<!>(a = A()) }
context(a: A, b: B) fun example3d2() { <!OVERLOAD_RESOLUTION_AMBIGUITY!>foo3_2<!>(a = A()) }

// scenario 4: subtyping
open class Parent
class Child : Parent()

context(a: Parent) fun foo4() { }
context(a: Child) <!CONTEXTUAL_OVERLOAD_SHADOWED!>fun foo4()<!> { }

context(child: Child) fun example4a() { <!OVERLOAD_RESOLUTION_AMBIGUITY!>foo4<!>() }
fun example4b() { <!OVERLOAD_RESOLUTION_AMBIGUITY!>foo4<!>(a = Child()) }

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, functionDeclarationWithContext, lambdaLiteral */
