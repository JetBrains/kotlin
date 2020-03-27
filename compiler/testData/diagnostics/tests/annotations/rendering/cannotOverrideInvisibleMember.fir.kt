// !DIAGNOSTICS: -INCOMPATIBLE_MODIFIERS
// !RENDER_DIAGNOSTICS_MESSAGES

@Target(AnnotationTarget.FUNCTION, AnnotationTarget.TYPE, AnnotationTarget.CLASS)
annotation class A

@A
open class B1 {
    @A
    <!INCOMPATIBLE_MODIFIERS!>private<!> <!INCOMPATIBLE_MODIFIERS!>open<!> fun foo() {}
}

class D1 : B1() {
    override fun foo() {}
}