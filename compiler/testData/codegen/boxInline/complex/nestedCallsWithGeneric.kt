// FILE: 1.kt
package foo

fun fail(s: String): Nothing = throw Error(s)

inline fun <T> depth1(a: Any?): T = a as T

inline fun <T> depth2(a: Any?): T = depth1<T>(a)

fun test() {
    val t11 = depth1<String?>(null)
    if (t11 != null) fail("t11")  
    val t12 = depth2<String?>(null)
    if (t12 != null) fail("t12")

    val t21 = depth1<Nothing?>(null)
    if (t21 != null) fail("t21")
    val t22 = depth2<Nothing?>(null)
    if (t22 != null) fail("t22")

    val t31 = depth1<String>("str1")
    if (t31 != "str1") fail("t31")
    val t32 = depth2<String>("str2")
    if (t32 != "str2") fail("t32")
}


// From KT-60113
fun kt60113() {
    stupidRemembered = ComposerEmpty
    crashes1_illegalcast()
    stupidRemembered = ComposerEmpty
    crashes2_unreachable()
    stupidRemembered = ComposerEmpty
    crash3_illegal_cast()
    nocrash()

    val expectedInL = "BEFORE_CAST - null;Text1 = null;BEFORE_CAST - null;Text2 = null;BEFORE_CAST - asdasd;Text3 = asdasd;BEFORE_CAST - asdasd;Text0 = asdasd;"
    if (l != expectedInL) fail("Unexpected value for l:$l")
}

var l = ""
fun log(t: String?) {
    l += t + ";"
}

fun crashes1_illegalcast() {
    val text = remember {
        if (false) {
            "asdasd"
        } else {
            null
        }
    }

    log("Text1 = $text")
}

fun crashes2_unreachable() {
    val text = remember {
        if (false) {
            null
        } else {
            null
        }
    }

    log("Text2 = $text")
}

fun crash3_illegal_cast() {
    val text = remember {
        if (true) {
            "asdasd"
        } else {
            "asda324234"
        }
    }

    log("Text3 = $text")
}

fun nocrash() {
    val text = cache(false) {
        if (false) {
            "asdasd"
        } else {
            null
        }
    }

    log("Text0 = $text")
}

inline fun <T> remember(crossinline calculation: () -> T): T =
    cache(false, calculation)

inline fun <T> cache(invalid: Boolean, block: () -> T): T {
    @Suppress("UNCHECKED_CAST")
    return rememberedValue().let {
        if (invalid || it === ComposerEmpty) {
            val value = block()
            stupidRemembered = value
            value
        } else it
    }.also { log("BEFORE_CAST - $it") } as T
}

fun rememberedValue(): Any? {
    return stupidRemembered
}

val ComposerEmpty = Any()

var stupidRemembered: Any? = ComposerEmpty

// FILE: 2.kt
package foo

fun box(): String {
    test()
    kt60113()

    return "OK"
}