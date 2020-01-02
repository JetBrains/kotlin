// !DIAGNOSTICS: -UNUSED_PARAMETER

inline operator fun <reified T> T.plus(p: T): T = this

inline operator fun <reified T> T.invoke(): T  = this

fun <A> main(tp: A, any: Any) {
    tp + tp
    any + any

    tp()
    any()
}