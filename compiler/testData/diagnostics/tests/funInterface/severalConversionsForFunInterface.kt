// !LANGUAGE: +NewInference +SamConversionForKotlinFunctions +SamConversionPerArgument +FunctionInterfaceConversion

interface J {
    fun foo1(r: KRunnable)

    fun foo2(r1: KRunnable, r2: KRunnable)

    fun foo3(r1: KRunnable, r2: KRunnable, r3: KRunnable)
}

fun interface KRunnable {
    fun run()
}

// FILE: 1.kt
fun test(j: J, r: KRunnable) {
    j.foo1(r)
    j.foo1({})

    j.foo2(r, r)
    j.foo2({}, {})
    j.foo2(r, {})
    j.foo2({}, r)

    j.foo3(r, r, r)
    j.foo3(r, r, {})
    j.foo3(r, {}, r)
    j.foo3(r, {}, {})
    j.foo3({}, r, r)
    j.foo3({}, r, {})
    j.foo3({}, {}, r)
    j.foo3({}, {}, {})
}