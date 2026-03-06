// IGNORE_IR_DESERIALIZATION_TEST: NATIVE
// ^^^ KT-73621: EVALUATED{IR} is missing
// WITH_STDLIB

object Test {
    fun foo(): String = "foo " + this

    fun bar(): String = "<!EVALUATED{IR}("bar ")!>bar <!>$this"

    fun baz(): String = "baz " + this.toString()
}

// STOP_EVALUATION_CHECKS
fun box(): String {
    if (!Test.foo().startsWith("foo ")) return "Fail ${Test.foo()}"
    if (!Test.bar().startsWith("bar ")) return "Fail ${Test.bar()}"
    if (!Test.baz().startsWith("baz ")) return "Fail ${Test.baz()}"
    return "OK"
}
