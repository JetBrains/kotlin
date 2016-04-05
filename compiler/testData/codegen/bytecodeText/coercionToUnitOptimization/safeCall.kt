interface A {
    fun foo()
}

fun test(x: A?) {
    x?.foo()
}

// 1 POP
// 0 ACONST_NULL
