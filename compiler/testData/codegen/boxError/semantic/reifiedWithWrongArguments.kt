// IGNORE_BACKEND: JS_IR, JS_IR_ES6
// IGNORE_ERRORS
// ERROR_POLICY: SEMANTIC

// MODULE: lib
// FILE: t.kt
import kotlin.reflect.KClass

var l = ""
fun log(s: String, r: Any? = null): Any? {
    l += s + ";"
    return r
}

inline fun assertFails(block: () -> Unit) {
    try {
        block()
    } catch (e: Throwable) {
        return
    }

    fail("Expected an exception to be thrown, but was completed successfully.")
}

fun <reified R> getKClassFromRaifed(b: Boolean): KClass<R>? {
    log("getKClassFromRaifed($b)")
    if (b) return R::class
    log("getKClassFromRaifed: null")
    return null
}

fun <T> getKClassFromT(b: Boolean): KClass<T>? {
    log("getKClassFromT($b)")
    if (b) return T::class
    log("getKClassFromT: null")
    return null
}

fun getKClassFromErrorType(b: Boolean): KClass<*>? {
    log("getKClassFromErrorType($b)")
    if (b) return ErrT::class
    log("getKClassFromErrorType: null")
    return null
}

inline fun <reified R, T> test1() {
    assertFails { getKClassFromRaifed<R>(true) }
    assertFails { getKClassFromRaifed<T>(true) }
    assertFails { getKClassFromRaifed<String>(true) }
    assertFails { getKClassFromT<R>(true) }
    assertFails { getKClassFromT<T>(true) }
    assertFails { getKClassFromT<String>(true) }
    assertFails { getKClassFromErrorType(true) }
    getKClassFromRaifed<R>(false)
    getKClassFromRaifed<T>(false)
    getKClassFromRaifed<String>(false)
    getKClassFromT<R>(false)
    getKClassFromT<T>(false)
    getKClassFromT<String>(false)
    getKClassFromErrorType(false)
}

inline fun <reified R> foo(a: Any, b: Any, c: Any): KClass<*>? {
    log("foo")
    return R::class
}

fun <T> testSideEffects() {
    assertFails { foo<T>(log("1", 1), log("2", 2), log("3", 3)) }
    foo<String>(log("a", 1), log("b", 2), log("c", 3))
}

// MODULE: main(lib)
// FILE: b.kt

fun box(): String {
    test1<Int, Short>()
    testSideEffects()


    val expected = "getKClassFromRaifed(true);getKClassFromRaifed(true);getKClassFromRaifed(true);" +
            "getKClassFromT(true);getKClassFromT(true);getKClassFromT(true);" +
            "getKClassFromErrorType(true);" +
            "getKClassFromRaifed(false);getKClassFromRaifed: null;getKClassFromRaifed(false);getKClassFromRaifed: null;getKClassFromRaifed(false);getKClassFromRaifed: null;" +
            "getKClassFromT(false);getKClassFromT: null;getKClassFromT(false);getKClassFromT: null;getKClassFromT(false);getKClassFromT: null;" +
            "getKClassFromErrorType(false);getKClassFromErrorType: null;" +
            "1;2;3;foo;a;b;c;foo;"

    if (l != expected)
        return "l = $l"

    return "OK"
}
