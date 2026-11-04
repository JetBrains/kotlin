// WITH_STDLIB
// LIBRARY_PLATFORMS: JVM
// LANGUAGE: +ContextParameters
package test

interface Ctx

fun primitives(vararg xs: Int) {}

fun nullablePrimitives(vararg xs: Int?) {}

fun nullableElements(vararg xs: String?) {}

fun <T> generic(vararg xs: T) {}

fun <T : Any> genericNotNull(vararg xs: T) {}

fun <T : CharSequence> bounded(vararg xs: T) {}

fun <T> genericLists(t: T, vararg xs: List<T>) {}

fun arrays(vararg xs: Array<String>) {}

fun primitiveArrays(vararg xs: IntArray) {}

fun lists(vararg xs: List<CharSequence>) {}

fun mutableLists(vararg xs: MutableList<CharSequence>) {}

fun finalElementLists(vararg xs: List<String>) {}

fun starLists(vararg xs: List<*>) {}

fun suppressedWildcards(vararg xs: @JvmSuppressWildcards List<CharSequence>) {}

fun elementWildcards(vararg xs: List<@JvmWildcard String>) {}

fun projectedArrays(vararg xs: Array<in String>) {}

fun functions(vararg xs: (CharSequence) -> CharSequence) {}

fun suspendFunctions(vararg xs: suspend () -> Unit) {}

fun String.extension(vararg xs: Int) {}

context(c: Ctx)
fun withContext(vararg xs: Int) {}

suspend fun suspending(vararg xs: String) {}

fun notLast(vararg xs: String, y: Int) {}

fun withDefault(vararg xs: String = arrayOf("a")) {}

@JvmOverloads
fun overloads(a: Int = 1, vararg xs: String = arrayOf()) {}

@JvmName("renamed")
fun named(vararg xs: String) {}

inline fun <reified T> reified(vararg xs: T) {}

private fun privateTopLevel(vararg xs: String) {}

internal fun internalTopLevel(vararg xs: String) {}

class Members(vararg val items: String) {
    constructor(x: Int, vararg ys: Long) : this()

    fun member(vararg xs: String) {}

    private fun privateMember(vararg xs: String) {}

    internal fun internalMember(vararg xs: String) {}

    companion object {
        @JvmStatic
        fun static(vararg xs: String) {}

        fun nonStatic(vararg xs: String) {}
    }
}

class Generic<T>(vararg val items: T)

class MutableProperty(vararg var items: String)

class Field(@JvmField vararg val items: String)

class AllDefaults(vararg val xs: String = arrayOf())

class OverloadedConstructor @JvmOverloads constructor(a: Int = 0, vararg xs: String)

class PrivateConstructor private constructor(vararg val xs: String)

enum class E(vararg val xs: Int) {
    A(1, 2),
    B,
}

interface I {
    fun abstractMember(vararg xs: String)

    fun withBody(vararg xs: String) {}
}

abstract class Base {
    abstract fun f(vararg xs: String)

    open fun g(vararg xs: Int?) {}

    protected open fun h(vararg xs: String) {}
}

class Derived : Base() {
    override fun f(vararg xs: String) {}

    override fun g(vararg xs: Int?) {}

    override fun h(vararg xs: String) {}
}

fun interface F {
    fun invoke(vararg xs: String)
}

annotation class A(vararg val value: String)

annotation class B(val x: Int, vararg val names: String)

object O {
    @JvmStatic
    fun static(vararg xs: Int) {}
}

// DECLARATIONS_NO_LIGHT_ELEMENTS: VarargParametersKt.class[reified]
// LIGHT_ELEMENTS_NO_DECLARATION: E.class[getEntries;valueOf;values]
