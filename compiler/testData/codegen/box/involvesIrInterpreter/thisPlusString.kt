// TARGET_BACKEND: JVM_IR
// TARGET_BACKEND: NATIVE
// WITH_STDLIB

object Test {
    fun foo(): String = "foo " + this

    fun bar(): String = "bar $this"

    fun baz(): String = "baz " + this.toString()
}

fun box(): String {
    if (!Test.foo().startsWith("foo ")) return "Fail ${Test.foo()}"
    if (!Test.bar().startsWith("bar ")) return "Fail ${Test.bar()}"
    if (!Test.baz().startsWith("baz ")) return "Fail ${Test.baz()}"
    return "OK"
}
