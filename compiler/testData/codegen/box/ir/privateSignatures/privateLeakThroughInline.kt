// MODULE: lib
// FILE: f1.kt

internal inline fun foo(): String = bar()

private fun bar(): String {
    return "11"
}

class C {
    internal inline fun fi(): String = bi()

    private fun bi(): String = "22"
}

private fun dex(): String = "33"

class CC {
    internal inline fun fx(): String = dex()
}

// FILE: f2.kt

fun test1(): String = foo()

fun test2(): String = C().fi()

fun test3(): String = CC().fx()


// MODULE: main(lib)
// FILE: m.kt

fun box(): String {
    if (test1() != "11") return "FAIL 1"
    if (test2() != "22") return "FAIL 2"
    if (test3() != "33") return "FAIL 3"

    return "OK"
}