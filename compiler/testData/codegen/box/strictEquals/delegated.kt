// LANGUAGE: +StrictEquals
// API_VERSION: 2.5
// DONT_TARGET_EXACT_BACKEND: NATIVE
//    ^^^ some native tests don't support API_VERSION directive
//    ^^^ fix or wait till version bump
// DUMP_IR

interface I {
    override fun equals(@EqualityBound(I::class) other: Any?): Boolean
}

class C : I {
    override fun equals(@EqualityBound(I::class) other: Any?): Boolean = true
}

class D : I by C()

fun box(): String {
    if (D() != D()) return "Fail#1"
    if (D() != C()) return "Fail#2"
    return "OK"
}
