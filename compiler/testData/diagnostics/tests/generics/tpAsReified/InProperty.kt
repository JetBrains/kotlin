val <reified T> v: T
    get() = throw UnsupportedOperationException()

fun <T> id(p: T): T = p

fun <A> main() {
    val <!UNUSED_VARIABLE!>a<!> = <!TYPE_INFERENCE_NO_INFORMATION_FOR_PARAMETER, DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>v<!>

    val <!UNUSED_VARIABLE!>b<!>: A = <!TYPE_PARAMETER_AS_REIFIED!>v<!>

    val <!UNUSED_VARIABLE!>c<!>: Int = v

    // TODO svtk, uncomment when extensions are called for nested calls!
    //val < !UNUSED_VARIABLE!>d< !>: A = id(< !TYPE_PARAMETER_AS_REIFIED!>v< !>)
}