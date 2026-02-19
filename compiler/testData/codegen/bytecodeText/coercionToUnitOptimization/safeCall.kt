interface A {
    fun foo()
}

fun test(x: A?) {
    x?.foo()
}

// 0 ACONST_NULL
