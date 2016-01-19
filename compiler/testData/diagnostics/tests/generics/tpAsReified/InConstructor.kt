class C<<!REIFIED_TYPE_PARAMETER_NO_INLINE!>reified<!> T>

fun <T> id(p: T): T = p

fun <A> main() {
    <!TYPE_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>C<!>()

    val <!UNUSED_VARIABLE!>a<!>: C<A> = <!TYPE_PARAMETER_AS_REIFIED!>C<!>()
    C<<!TYPE_PARAMETER_AS_REIFIED!>A<!>>()

    val <!UNUSED_VARIABLE!>b<!>: C<Int> = C()
    C<Int>()

    // TODO svtk, uncomment when extensions are called for nested calls!
    //val < !UNUSED_VARIABLE!>с< !>: C<A> = id(< !TYPE_PARAMETER_AS_REIFIED!>C< !>())
}