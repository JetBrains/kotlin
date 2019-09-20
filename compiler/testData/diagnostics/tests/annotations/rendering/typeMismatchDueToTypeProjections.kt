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
    a + <!TYPE_MISMATCH_DUE_TO_TYPE_PROJECTIONS("Out<Nothing>", "Out<CharSequence>", "C<out CharSequence>", "public final operator fun plus(x: Out<T>): C<T> defined in C")!>y<!>
}