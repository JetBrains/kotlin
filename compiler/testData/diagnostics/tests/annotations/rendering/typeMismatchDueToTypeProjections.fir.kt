// !DIAGNOSTICS: -UNUSED_PARAMETER
// !RENDER_DIAGNOSTICS_MESSAGES

@Target(AnnotationTarget.FUNCTION, AnnotationTarget.TYPE, AnnotationTarget.CLASS)
annotation class A

@A
class C<T> {
    @A
    operator fun plus(x: Out<@A T>): @A C<@A T> = this
}

class Out<out F>

fun test(a: C<out CharSequence>, y: Out<CharSequence>) {
    a + <!ARGUMENT_TYPE_MISMATCH("Out<@R|A|()  CapturedType(out kotlin/CharSequence)>; Out<kotlin/CharSequence>")!>y<!>
}
