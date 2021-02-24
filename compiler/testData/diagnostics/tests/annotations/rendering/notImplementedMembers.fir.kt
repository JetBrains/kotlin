// !RENDER_DIAGNOSTICS_MESSAGES

@Target(AnnotationTarget.FUNCTION, AnnotationTarget.TYPE, AnnotationTarget.CLASS,  AnnotationTarget.VALUE_PARAMETER,  AnnotationTarget.PROPERTY)
annotation class An

@An
interface A {
    @An
    fun a(@An arg: @An Int)
}

@An
interface B {
    @An
    fun <T> a(@An arg: @An Int)
}

interface C : A, B

@An
abstract class D {
    @An
    abstract val d: @An Int
}

class E : D(), A
class F : A

@An
interface G {
    @An
    fun a(@An arg: @An Int)
}

@An
interface AI : A {
    @An
    override fun a(@An arg: @An Int) {}
}

@An
interface GI : G {
    @An
    override fun a(@An arg: @An Int) {}
}

class AG1(val a: A, val g: G) : A by a, G by g
class AG2() : AI, GI
