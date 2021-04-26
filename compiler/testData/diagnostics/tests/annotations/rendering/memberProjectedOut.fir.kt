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
    a[1] = <!ARGUMENT_TYPE_MISMATCH!>25<!>
}
