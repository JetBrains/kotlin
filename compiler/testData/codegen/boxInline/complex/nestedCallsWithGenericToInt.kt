// IGNORE_INLINER: IR

// FILE: 1.kt
package foo

fun fail(s: String): Nothing = throw Error(s)

inline fun <T> depth1(a: Any?): T = a as T

inline fun <T> depth2(a: Any?): T = depth1<T>(a)

fun test() {
    val t11 = depth1<Int?>(null)
    if (t11 != null) fail("t11")
    val t12 = depth2<Int?>(null)
    if (t12 != null) fail("t12")

    val t21 = depth1<Int?>(functionThatReturnsNull())
    if (t21 != null) fail("t21")
    val t22 = depth2<Int?>(functionThatReturnsNull())
    if (t22 != null) fail("t22")

    val t31 = depth1<Int>(3311)
    if (t31 != 3311) fail("t31")
    val t32 = depth2<Int?>(33222)
    if (t32 != 33222) fail("t32")
}

// From KT-60496
fun kt60496() {
    stupidRemembered = ComposerEmpty
    case1() 
    stupidRemembered = ComposerEmpty
    case2()
    stupidRemembered = ComposerEmpty
    case3()

    val expectedInL = "BEFORE_CAST - null;case1;BEFORE_CAST - null;case2;BEFORE_CAST - kotlin.Unit;case3;"
    if (l != expectedInL) fail("Unexpected value for l:$l")
}

var l = ""
fun log(t: String?) {
    l += t + ";"
}

fun case1() {
    var counter: Int? = remember { null }

    log("case1")
}

fun case2() {
    var counter: Int? = remember { functionThatReturnsNull() }

    log("case2")
}

fun case3() {
    var counter: Unit = remember { null }

    log("case3")
}

fun <T> functionThatReturnsNull(): T? = null

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
    kt60496()

    return "OK"
}
