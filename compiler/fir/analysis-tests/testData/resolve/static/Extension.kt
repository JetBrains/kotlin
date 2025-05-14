// RUN_PIPELINE_TILL: FRONTEND
// DIAGNOSTICS: -NO_COMPANION_OBJECT

class Example

fun ::Example.foo(): Int = 0
fun usesFoo1(): Int = Example.foo()
fun usesFoo2(): Int = Example().<!UNRESOLVED_REFERENCE!>foo<!>()

fun Example.moo(): Int = 1
fun usesMoo1(): Int = Example.<!UNRESOLVED_REFERENCE!>moo<!>()
fun usesMoo2(): Int = Example().moo()