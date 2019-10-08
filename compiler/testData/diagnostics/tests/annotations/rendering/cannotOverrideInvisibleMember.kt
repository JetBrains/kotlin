// !DIAGNOSTICS: -INCOMPATIBLE_MODIFIERS
// !RENDER_DIAGNOSTICS_MESSAGES

@Target(AnnotationTarget.FUNCTION, AnnotationTarget.TYPE, AnnotationTarget.CLASS)
annotation class A

@A
open class B1 {
    @A
    private open fun foo() {}
}

class D1 : B1() {
    <!CANNOT_OVERRIDE_INVISIBLE_MEMBER("public open fun foo(): Unit defined in D1", "private open fun foo(): Unit defined in B1")!>override<!> fun foo() {}
}