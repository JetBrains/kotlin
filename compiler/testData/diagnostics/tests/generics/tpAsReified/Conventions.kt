// !DIAGNOSTICS: -UNUSED_PARAMETER

inline operator fun <reified T> T.plus(p: T): T = this

inline operator fun <reified T> T.invoke(): T  = this

fun <A> main(tp: A, any: Any) {
    tp <!TYPE_PARAMETER_AS_REIFIED!>+<!> tp
    any + any

    <!TYPE_PARAMETER_AS_REIFIED!>tp<!>()
    any()
}