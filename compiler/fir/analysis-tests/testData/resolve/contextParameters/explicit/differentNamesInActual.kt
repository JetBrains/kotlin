// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +ContextParameters +ExplicitContextArguments  +MultiPlatformProjects

// MODULE: m1-common
// FILE: common.kt
class Ctx

expect interface A {
    context(_: Ctx)
    fun foo()
}

expect interface B {
    context(c: Ctx)
    fun foo()
}

expect interface C {
    context(c: Ctx)
    fun foo()
}

expect interface D {
    context(c: Ctx)
    fun foo()
}

fun bar0(b: B, c: C, d: D) {
    b.foo(c = Ctx())
    c.foo(c = Ctx())
    d.foo(c = Ctx())
}


// MODULE: m2-jvm()()(m1-common)
// FILE: JavaD.java
public interface JavaD {
    void foo(Ctx c)
}

// FILE: jvm.kt
actual interface A {
    context(c: Ctx)
    actual fun <!EXPECT_ACTUAL_INCOMPATIBLE_CONTEXT_PARAMETER_NAMES!>foo<!>()
}

actual interface B {
    context(ctx: Ctx)
    actual fun <!EXPECT_ACTUAL_INCOMPATIBLE_CONTEXT_PARAMETER_NAMES!>foo<!>()
}

actual interface C {
    context(_: Ctx)
    actual fun <!EXPECT_ACTUAL_INCOMPATIBLE_CONTEXT_PARAMETER_NAMES!>foo<!>()
}

actual typealias <!NO_ACTUAL_CLASS_MEMBER_FOR_EXPECTED_CLASS!>D<!> = JavaD


/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, functionDeclarationWithContext, interfaceDeclaration,
stringLiteral */
