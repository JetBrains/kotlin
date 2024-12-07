// RUN_PIPELINE_TILL: FRONTEND
// CHECK_TYPE
// DIAGNOSTICS: -UNUSED_EXPRESSION -UNUSED_PARAMETER

class Outer<out E> {
    inner class Inner {
        fun foo() = this
        fun baz(): Inner = this
    }

    fun bar(): Inner = Inner()

    fun set(inner: <!TYPE_VARIANCE_CONFLICT_ERROR!>Inner<!>) {}
}

fun factoryString(): Outer<String>.Inner = null!!

fun <T> infer(x: T): Outer<T>.Inner = null!!
val inferred = infer("")

fun main() {
    val outer = Outer<String>()

    checkSubtype<Outer<String>.Inner>(outer.bar())
    checkSubtype<Outer<String>.Inner>(outer.Inner())
    checkSubtype<Outer<*>.Inner>(outer.bar())
    checkSubtype<Outer<*>.Inner>(outer.Inner())

    checkSubtype<Outer<CharSequence>.Inner>(outer.bar())
    checkSubtype<Outer<CharSequence>.Inner>(outer.Inner())

    outer.set(outer.bar())
    outer.set(outer.Inner())

    val x: Outer<String>.Inner = factoryString()
    outer.set(x)
}
