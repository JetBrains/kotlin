// RUN_PIPELINE_TILL: FRONTEND
// FIR_DUMP

class Foo {
    companion object
}

class UsingStatic {
    fun ::Foo.example(): Boolean = true
}

class UsingCompanion {
    fun Foo.Companion.example(): Int = 3
}

fun <A, R> with(x: A, block: A.() -> R): R = block(x)

fun checkMoreNestedIsStatic(): Boolean =
    with(UsingCompanion()) {
        with(UsingStatic()) {
            Foo.example()
        }
    }

fun checkMoreNestedIsCompanion(): Int =
    with(UsingStatic()) {
        with(UsingCompanion()) {
            Foo.example() + 1
        }
    }

val fail: Boolean = <!INITIALIZER_TYPE_MISMATCH!>2<!>
