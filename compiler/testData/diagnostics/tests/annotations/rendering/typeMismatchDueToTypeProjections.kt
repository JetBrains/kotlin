// !DIAGNOSTICS: -UNUSED_PARAMETER
// !RENDER_DIAGNOSTICS_MESSAGES
// !WITH_NEW_INFERENCE

@Target(AnnotationTarget.FUNCTION, AnnotationTarget.TYPE, AnnotationTarget.CLASS)
annotation class A

@A
class C<T> {
    @A
    operator fun plus(x: Out<@A T>): @A C<@A T> = this
}

class Out<out F>

fun test(a: C<out CharSequence>, y: Out<CharSequence>) {
    a + <!TYPE_MISMATCH{NI}("Out<Nothing>; Out<CharSequence>"), TYPE_MISMATCH_DUE_TO_TYPE_PROJECTIONS{OI}!>y<!>
}
