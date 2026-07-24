// IDE_MODE
// LANGUAGE: +ContextSensitiveResolutionUsingExpectedType

// FILE: top.kt
package test

class Top {
    enum class Foo { BAR }
}

// FILE: main.kt
package main

fun usage(): test.Top.Foo {
    // Only the last name `BAR` is CSR-removable (the whole `test.Top.Foo.` qualifier is dropped at once).
    // The middle segments `test`, `Top`, `Foo` must stay NotAvailable: CSR never shortens partially.
    return test.Top.Foo.BAR
}
