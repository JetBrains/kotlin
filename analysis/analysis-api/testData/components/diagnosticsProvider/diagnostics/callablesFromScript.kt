// IGNORE_FE10

// MODULE: lib
// MODULE_KIND: ScriptSource
// FILE: import.kts

val a = 42

class A

enum class E {
    V
}

object O {
    val v = 42
}

fun foo() = 42

// MODULE: main(lib)
// MODULE_KIND: ScriptSource
// FILE: main.kts
a

A()

E.V

O.v

foo()


