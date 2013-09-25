fun <reified T> f(): T = throw UnsupportedOperationException()

fun <T> id(p: T): T = p

fun <A> main() {
    <!TYPE_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>f<!>()

    val <!UNUSED_VARIABLE!>a<!>: A = <!TYPE_PARAMETER_AS_REIFIED!>f<!>()
    <!TYPE_PARAMETER_AS_REIFIED!>f<!><A>()

    val <!UNUSED_VARIABLE!>b<!>: Int = f()
    f<Int>()

    // TODO svtk, uncomment when extensions are called for nested calls!
    //val < !UNUSED_VARIABLE!>с< !>: A = id(< !TYPE_PARAMETER_AS_REIFIED!>f< !>())
}