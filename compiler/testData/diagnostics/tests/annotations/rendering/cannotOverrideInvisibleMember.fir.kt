// RUN_PIPELINE_TILL: FRONTEND
// DIAGNOSTICS: -INCOMPATIBLE_MODIFIERS
// RENDER_DIAGNOSTICS_MESSAGES

@Target(AnnotationTarget.FUNCTION, AnnotationTarget.TYPE, AnnotationTarget.CLASS)
annotation class A

@A
open class B1 {
    @A
    private open fun foo() {}
}

class D1 : B1() {
    <!NOTHING_TO_OVERRIDE("foo;  Potential signatures for overriding:fun foo(): Unit")!>override<!> fun foo() {}
}
