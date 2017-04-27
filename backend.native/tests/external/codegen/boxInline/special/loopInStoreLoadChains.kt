// NO_CHECK_LAMBDA_INLINING

// FILE: 1.kt
inline fun test(noinline foo: () -> String, noinline bar: () -> String): String {
    var vfoo = foo
    var vbar = bar
    var vres = foo
    for (i in 1 .. 4) {
        vres = vbar
        vbar = vfoo
        vfoo = vres
    }
    return vres()
}


// FILE: 2.kt
fun box(): String =
        test({ "OK" }, { "wrong lambda" })