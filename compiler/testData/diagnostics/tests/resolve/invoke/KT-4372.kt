// !DIAGNOSTICS: -UNUSED_PARAMETER

//KT-4372 Invalid error position and failing to resolve invoke with receiver from other module

class Foo<TInner, TOuter> {
    fun invoke(content: TInner.() -> Unit) {
    }
}

// comment this function to fix the error below
fun <TInner, TOuter> Foo<TInner, TOuter>.invoke(name: String, content: TInner.() -> Unit) {}

enum class EnumClass(val x: String) {}
object Y {
    val x = javaClass<EnumClass>() // javaClass unresolved in any file in this module
}

//declarations from library
val <T> T.javaClass : Class<T>
    get() = throw Exception()

fun <<!REIFIED_TYPE_PARAMETER_NO_INLINE!>reified<!> T> javaClass() : Class<T> = throw Exception()
