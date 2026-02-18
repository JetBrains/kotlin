// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +ContextParameters +ExplicitContextArguments

// FILE: interfaces.kt

interface Ctx

interface I1 {
    fun foo()

    context(ctx: Ctx)
    <!CONTEXTUAL_OVERLOAD_SHADOWED!>fun foo()<!>
}

interface I2 {
    fun foo() {}
}

interface I3 {
    context(ctx: Ctx)
    fun foo() {}
}

// FILE: J1.java

public class J1 implements I1 {
    @Override
    public void foo() {}

    @Override
    public void foo(Ctx ctx) {}
}

// FILE: J2.java

public class J2 implements I2, I3 {
}

// FILE: test.kt

class K1 : J1()
class K2 : J2()
class K3 : I2, I3
class K4 : I2
class K5 : I3

fun test1() {
    J1().foo()
    J2().foo()
    K1().foo()
    K2().foo()
    K3().foo()
    K4().foo()
    K5().<!NO_CONTEXT_ARGUMENT!>foo<!>()
}

context(ctx: Ctx)
fun test2() {
    J1().<!OVERLOAD_RESOLUTION_AMBIGUITY!>foo<!>()
    J2().<!OVERLOAD_RESOLUTION_AMBIGUITY!>foo<!>()
    K1().<!OVERLOAD_RESOLUTION_AMBIGUITY!>foo<!>()
    K2().<!OVERLOAD_RESOLUTION_AMBIGUITY!>foo<!>()
    K3().<!OVERLOAD_RESOLUTION_AMBIGUITY!>foo<!>()
    K4().foo()
    K5().foo()
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, functionDeclarationWithContext, interfaceDeclaration,
javaFunction, javaType */
