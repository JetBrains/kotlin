// TARGET_BACKEND: JVM
// JVM_TARGET: 1.8
// SAM_CONVERSIONS: INDY
// FILE: 1.kt
inline fun cross(crossinline fn: () -> String) : Any =
    object {
        override fun toString(): String = fn()
    }

fun interface IFoo {
    fun foo(): String
}

fun foo(iFoo: IFoo) = iFoo.foo()

fun bar(f: (Array<String>) -> Unit): Any = f

inline fun make(crossinline f: (String) -> Unit): Any = bar { f(it[0]) }

// FILE: 2.kt
private fun getBar() = bar { it.size }

private fun getMake() = make { it.length }

fun box(): String {
    if (cross { foo { "OK" } }.toString() != "OK") return "Crossinline behavior failed"

    if (getBar() !== getBar()) return "Stateless lambda was not reused"
    if (getMake() !== getMake()) return "Regenerated stateless lambda was not reused"
    if (getMake() === make { it.length }) return "Different lambdas were reused"

    return "OK"
}
