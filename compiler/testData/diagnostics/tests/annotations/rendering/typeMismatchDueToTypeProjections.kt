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
    a + <!"C<out{OI}, "Out<CharSequence>"{OI}, "Out<Nothing>"{OI}, "public{OI}, :{OI}, C"{OI}, C<T>{OI}, CharSequence>"{OI}, NI{OI}, Out<T>{OI}, TYPE_MISMATCH("Out<Nothing>; Out<CharSequence>"), TYPE_MISMATCH_DUE_TO_TYPE_PROJECTIONS{OI}, defined{OI}, final{OI}, fun{OI}, in{OI}, operator{OI}, plus{OI}, x:{OI}!>y<!>
}
