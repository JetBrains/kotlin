// TARGET_BACKEND: JVM
// JVM_TARGET: 1.8
// SAM_CONVERSIONS: INDY
// FILE: 1.kt
class C {
    fun test() =
        cross {
            foo { "OK" }
        }.toString()
}

inline fun cross(crossinline fn: () -> String) : Any =
    object {
        override fun toString(): String = fn()
    }

fun interface IFoo {
    fun foo(): String
}

fun foo(iFoo: IFoo) = iFoo.foo()

// FILE: 2.kt
fun box() =
    C().test()

