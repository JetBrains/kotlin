// RUN_PIPELINE_TILL: FRONTEND
// FIR_DUMP

class Example {
    static fun foo(): Int = 2

    companion object {
        fun foo(): Boolean = true
        fun onlyCompanion(): Boolean = false
    }
}

val x: Int = Example.foo()
val y: Boolean = Example.Companion.foo()

fun ::Example.moo1(): Int = Example.foo()
fun ::Example.moo2(): Int = foo()
fun Example.Companion.moo3(): Boolean = foo()

fun ::Example.bar1(): Boolean = Example.onlyCompanion()
fun ::Example.bar2(): Boolean = <!UNRESOLVED_REFERENCE!>onlyCompanion<!>()
fun Example.Companion.bar3(): Boolean = onlyCompanion()