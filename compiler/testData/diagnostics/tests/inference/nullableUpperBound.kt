// FIR_IDENTICAL
package o

fun foo(): String? {
    return accept(JV<String?, Unit?>())
}

fun <R, D> accept(v: JV<R, D>): R? = null

open class JV<R, D>()
