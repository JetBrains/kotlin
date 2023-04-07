// TARGET_BACKEND: JVM_IR
// TARGET_BACKEND: NATIVE
// TARGET_BACKEND: JS_IR
// WITH_STDLIB

object Test {
    fun foo(): String = "foo " + this

    fun bar(): String = "bar $this"

    fun baz(): String = "baz " + this.toString()
}

fun box(): String {
    if (!Test.foo().startsWith("foo ")) return "<!EVALUATED("Fail ")!>Fail <!>${Test.foo()}"
    if (!Test.bar().startsWith("bar ")) return "<!EVALUATED("Fail ")!>Fail <!>${Test.bar()}"
    if (!Test.baz().startsWith("baz ")) return "<!EVALUATED("Fail ")!>Fail <!>${Test.baz()}"
    return "OK"
}
