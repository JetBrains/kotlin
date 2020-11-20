// FIR_IDENTICAL
// !DIAGNOSTICS: -UNUSED_PARAMETER

open class A {
    open val servers: List<C>
        get() = findAndExpand({ "hi" })
            .mapNotNull { B.foo(it) }

    fun findAndExpand(vararg path: () -> String): List<String> = TODO()
}

object B {
    inline fun <reified T : C> foo(bar: String?): T? = TODO()
}

open class C
class D : C()