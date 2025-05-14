// RUN_PIPELINE_TILL: FRONTEND
// DIAGNOSTICS: -NO_COMPANION_OBJECT

fun ::Example.staticExtension(): Boolean = true
fun Example.memberExtension(): Boolean = false

class Example {
    static fun foo(): Boolean = staticExtension()
    static fun bar(): Boolean = <!UNRESOLVED_REFERENCE!>memberExtension<!>()

    fun moo1(): Boolean = staticExtension()
    fun moo2(): Boolean = memberExtension()
}

fun ::Example.usesFoo1(): Boolean = Example.foo()
fun ::Example.usesFoo2(): Boolean = foo()