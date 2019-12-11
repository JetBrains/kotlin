// !DIAGNOSTICS: -DEPRECATION -TOPLEVEL_TYPEALIASES_ONLY

fun test(`_`: Int) {
    _ + 1
    `_` + 1
}

fun `__`() {}

fun testCall() {
    __()
    `__`()
}

val testCallableRef = ::__
val testCallableRef2 = ::`__`


object Host {
    val `_` = 42
    object `__` {
        val bar = 4
    }
}

val testQualified = Host._
val testQualified2 = Host.`_`

object `___` {
    val test = 42
}

val testQualifier = ___.test
val testQualifier2 = `___`.test
val testQualifier3 = Host.__.bar
val testQualifier4 = Host.`__`.bar

fun testCallableRefLHSValue(`_`: Any) = _::toString
fun testCallableRefLHSValue2(`_`: Any) = `_`::toString

val testCallableRefLHSObject = ___::toString
val testCallableRefLHSObject2 = `___`::toString
