fun foo() = 42

class Your {
    fun bar() = 13
}

class My {
    val your = Your()

    val x = foo()

    val y = Your().bar()

    val z = your.bar()

    // This extension function also can use our properties,
    // so the call is also dangerous
    val w = your.<!DEBUG_INFO_LEAKING_THIS!>gav<!>()

    val v = Your().<!DEBUG_INFO_LEAKING_THIS!>gav<!>()

    val t = your.other()

    val r = Your().other()

    fun Your.gav() = if (your.bar() == 0) t else r
}

fun Your.other() = "3"