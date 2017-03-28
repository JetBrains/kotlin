// !DIAGNOSTICS: -DEPRECATION -TOPLEVEL_TYPEALIASES_ONLY

fun test(`_`: Int) {
    <!UNDERSCORE_USAGE_WITHOUT_BACKTICKS!>_<!> + 1
    `_` + 1
}

fun `__`() {}

fun testCall() {
    <!UNDERSCORE_USAGE_WITHOUT_BACKTICKS!>__<!>()
    `__`()
}

val testCallableRef = ::<!UNDERSCORE_USAGE_WITHOUT_BACKTICKS!>__<!>
val testCallableRef2 = ::`__`


object Host {
    val `_` = 42
    object `__` {
        val bar = 4
    }
}

val testQualified = Host.<!UNDERSCORE_USAGE_WITHOUT_BACKTICKS!>_<!>
val testQualified2 = Host.`_`

object `___` {
    val test = 42
}

val testQualifier = <!UNDERSCORE_USAGE_WITHOUT_BACKTICKS!>___<!>.test
val testQualifier2 = `___`.test
val testQualifier3 = Host.<!UNDERSCORE_USAGE_WITHOUT_BACKTICKS!>__<!>.bar
val testQualifier4 = Host.`__`.bar

fun testCallableRefLHSValue(`_`: Any) = <!UNDERSCORE_USAGE_WITHOUT_BACKTICKS!>_<!>::toString
fun testCallableRefLHSValue2(`_`: Any) = `_`::toString

val testCallableRefLHSObject = <!UNDERSCORE_USAGE_WITHOUT_BACKTICKS!>___<!>::toString
val testCallableRefLHSObject2 = `___`::toString
