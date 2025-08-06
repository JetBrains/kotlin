// TARGET_BACKEND: JVM
// WITH_STDLIB
// JVM_DEFAULT_MODE: disable

package test

interface CookieJar {
    companion object {
        @JvmField
        val NO_COOKIES: CookieJar = object : CookieJar { }
    }
}

fun box(): String {
    val enclosing = CookieJar.NO_COOKIES::class.java.enclosingClass.name
    if (enclosing != "test.CookieJar") return "Fail: $enclosing"

    return "OK"
}
