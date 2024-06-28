// FIR_IDENTICAL
fun foo(x: String = ""): String = x

class C(val x: String = "")

fun use(fn: () -> Any) = fn()

fun testFn() = use(::foo)

fun testCtor() = use(::C)