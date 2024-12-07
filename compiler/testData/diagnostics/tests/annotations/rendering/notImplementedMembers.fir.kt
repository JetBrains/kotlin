// RUN_PIPELINE_TILL: FRONTEND
// RENDER_DIAGNOSTICS_MESSAGES

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

<!CONFLICTING_INHERITED_MEMBERS("C; 'fun a(arg: @An() Int): Unit' defined in '/A', 'fun <T> a(arg: @An() Int): Unit' defined in '/B'")!>interface C<!> : A, B

@An
abstract class D {
    @An
    abstract val d: @An Int
}

<!ABSTRACT_CLASS_MEMBER_NOT_IMPLEMENTED("Class 'E'; member:val d: @An() Int"), ABSTRACT_MEMBER_NOT_IMPLEMENTED("Class 'E'; member:fun a(arg: @An() Int): Unit")!>class E<!> : D(), A
<!ABSTRACT_MEMBER_NOT_IMPLEMENTED("Class 'F'; member:fun a(arg: @An() Int): Unit")!>class F<!> : A

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

<!MANY_IMPL_MEMBER_NOT_IMPLEMENTED("Class 'AG1'; a")!>class AG1<!>(val a: A, val g: G) : A by a, G by g
<!CANNOT_INFER_VISIBILITY("a"), MANY_INTERFACES_MEMBER_NOT_IMPLEMENTED("Class 'AG2'; a")!>class AG2<!>() : AI, GI
