package o

fun foo(): String? {
    return accept(JV<String?, Unit?>())
}

fun <R, D> accept(<!UNUSED_PARAMETER!>v<!>: JV<R, D>): R? = null

open class JV<R, D>()