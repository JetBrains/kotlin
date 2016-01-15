// !CHECK_TYPE
// !DIAGNOSTICS: -UNUSED_EXPRESSION -UNUSED_PARAMETER

class Outer<E> {
    inner class Inner {
        fun foo() = this
        fun baz(): Inner = this
    }

    fun bar() = Inner()

    fun set(inner: Inner) {}
}

fun factoryString(): Outer<String>.Inner = null!!

fun <T> infer(x: T): Outer<T>.Inner = null!!
val infered = infer("")

fun main() {
    val outer: Outer<out String> = Outer<String>()

    checkSubtype<Outer<out String>.Inner>(outer.bar())
    checkSubtype<Outer<out String>.Inner>(outer.Inner())
    checkSubtype<Outer<*>.Inner>(outer.bar())
    checkSubtype<Outer<*>.Inner>(outer.Inner())

    checkSubtype<Outer<out CharSequence>.Inner>(outer.bar())
    checkSubtype<Outer<out CharSequence>.Inner>(outer.Inner())

    outer.<!MEMBER_PROJECTED_OUT!>set<!>(outer.bar())
    outer.<!MEMBER_PROJECTED_OUT!>set<!>(outer.Inner())

    val x: Outer<String>.Inner = factoryString()
    outer.<!MEMBER_PROJECTED_OUT!>set<!>(x)
}
