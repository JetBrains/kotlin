// WITH_STDLIB
open class A {
    val a: String.() -> String = { this }
    val b: (String) -> String by this::a
}

class B {
    val b: (String) -> String by A()::a
}

class C: A() {
    val c: (String) -> String by ::a
}

fun box(): String {
    return A().b("O") + B().b("K") + C().c("")
}