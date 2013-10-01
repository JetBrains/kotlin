fun <reified T> T.plus(<!UNUSED_PARAMETER!>p<!>: T): T = this

fun <reified T> T.invoke(): T  = this

fun <A> main(tp: A, any: Any) {
    tp <!TYPE_PARAMETER_AS_REIFIED!>+<!> tp
    any + any

    <!TYPE_PARAMETER_AS_REIFIED!>tp<!>()
    any()
}