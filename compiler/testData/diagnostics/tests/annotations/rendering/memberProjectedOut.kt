// !DIAGNOSTICS: -UNUSED_PARAMETER
// !WITH_NEW_INFERENCE
// !RENDER_DIAGNOSTICS_MESSAGES

@Target(AnnotationTarget.FUNCTION, AnnotationTarget.TYPE, AnnotationTarget.CLASS)
annotation class A

@A
class C<T> {
    @A
    operator fun set(x: @A Int, y: @A T) {}
}

fun test(a: C<out CharSequence>) {
    <!MEMBER_PROJECTED_OUT{OI}("public final operator fun set(x: Int, y: T): Unit defined in C; C<out CharSequence>")!>a[1]<!> = <!CONSTANT_EXPECTED_TYPE_MISMATCH{NI}("integer; Nothing")!>25<!>
}
