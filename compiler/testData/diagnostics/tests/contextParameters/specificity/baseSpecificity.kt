// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +ContextParameters +ExplicitContextArguments

open class A
class SubA : A()
class B

fun <A, R> context(context: A, block: context(A) () -> R): R = block(context)

// scenario 1: base scenario
fun foo1a() { }

context(a: A)
<!CONTEXTUAL_OVERLOAD_SHADOWED!>fun foo1a()<!> { }

val prop1a get() = 42

context(a: A)
<!CONTEXTUAL_OVERLOAD_SHADOWED!>val prop1a<!> get() = 42

context(b: B)
fun foo1b() { }

context(b: B, a: A)
<!CONTEXTUAL_OVERLOAD_SHADOWED!>fun foo1b()<!> { }

context(b: B)
val prop1b get() = 42

context(b: B, a: A)
<!CONTEXTUAL_OVERLOAD_SHADOWED!>val prop1b<!> get() = 42

context(a: A)
fun foo1c() { }

context(b: B)
fun foo1c() { }

context(a: A)
val prop1c get() = 42

context(b: B)
val prop1c get() = 42

context(a: A, b: B)
<!CONFLICTING_OVERLOADS!>fun foo1d()<!> { }

context(d: B, c: A)
<!CONFLICTING_OVERLOADS!>fun foo1d()<!> { }

context(a: A, b: B)
val <!REDECLARATION!>prop1d<!> get() = 42

context(d: B, _: A)
val <!REDECLARATION!>prop1d<!> get() = 42

// scenario 2: context vs. value parameter
context(a: A)
fun foo2() { }

fun foo2(a: A) { }

// scenario 3: context vs. optional parameter
context(a: A)
fun foo3a() { }

fun foo3a(a: A = A()) { }


context(b: B, a: A)
fun foo3b() { }

context(b: B)
fun foo3b(a: A = A()) { }


context(a: A)
fun foo3c() { }

fun foo3c(b: A = A()) { }

// scenario 4: context vs. vararg parameter
context(ctx: A)
fun foo4a() { }

fun foo4a(vararg a: A) { }

fun foo4b() { }

context(ctx: A)
fun foo4b(vararg a: A) { }

// scenario 5: context vs. receiver
context(a: SubA)
fun foo5(): Int = 42

fun A.foo5(): String = ""

context(a: SubA)
val prop5: Int get() = 42

val A.prop5: String get() = ""

fun test0() {
    foo1a()
    prop1a
    foo1a(a = A())

    <!OVERLOAD_RESOLUTION_AMBIGUITY!>foo2<!>(a = A())

    <!OVERLOAD_RESOLUTION_AMBIGUITY!>foo3a<!>(a = A())
    foo3c()
    foo3c(a = A())
    foo3c(b = A())

    foo4a()
    foo4b()
}

context(a: A)
fun test1() {
    <!OVERLOAD_RESOLUTION_AMBIGUITY!>foo1a<!>()
    <!OVERLOAD_RESOLUTION_AMBIGUITY!>prop1a<!>
    foo1a(a = A())
    foo1c()
    prop1c

    foo2()
    <!OVERLOAD_RESOLUTION_AMBIGUITY!>foo2<!>(a = A())

    foo3a()
    <!OVERLOAD_RESOLUTION_AMBIGUITY!>foo3a<!>(a = A())
    foo3c()
    foo3c(a = A())
    foo3c(b = A())

    foo4a()
    foo4a(A())
    foo4b()
    foo4b(ctx = A())
}

context(b: B)
fun test2() {
    foo1c()
    prop1c

    <!OVERLOAD_RESOLUTION_AMBIGUITY!>foo3b<!>(a = A())
}

context(a: A, b: B)
fun test3() {
    <!OVERLOAD_RESOLUTION_AMBIGUITY!>foo1b<!>()
    <!OVERLOAD_RESOLUTION_AMBIGUITY!>prop1b<!>
    foo1b(a = A())
    <!OVERLOAD_RESOLUTION_AMBIGUITY!>foo1c<!>()
    <!OVERLOAD_RESOLUTION_AMBIGUITY!>prop1c<!>

    foo3b()
    <!OVERLOAD_RESOLUTION_AMBIGUITY!>foo3b<!>(a = A())
}

fun test4(arg: Any) {
    foo1a()
    prop1a
    context(arg) {
        foo1a()
        prop1a
    }
    if (arg is A) {
        <!CANNOT_INFER_PARAMETER_TYPE!>context<!>(arg) {
            <!OVERLOAD_RESOLUTION_AMBIGUITY!>foo1a<!>()
            <!OVERLOAD_RESOLUTION_AMBIGUITY!>prop1a<!>
        }
    }
}

fun test5() {
    <!NO_CONTEXT_ARGUMENT!>foo5<!>()
    <!NO_CONTEXT_ARGUMENT!>prop5<!>
    with(SubA()) {
        val t0 = foo5()
        val t1 = prop5
    }
    context(SubA()) {
        val t0 = foo5()
        val t1 = prop5
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, functionDeclarationWithContext */
