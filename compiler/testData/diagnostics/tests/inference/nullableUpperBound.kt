package o

fun foo(): String? {
    return accept(JV<String?, Unit?>())
}

fun <R, D> accept(<!UNUSED_PARAMETER!>v<!>: JV<R, D>): R<!BASE_WITH_NULLABLE_UPPER_BOUND!>?<!> = null

open class JV<R, D>()