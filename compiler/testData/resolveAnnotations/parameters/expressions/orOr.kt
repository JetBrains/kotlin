package test

annotation class Ann(
        val b1: Boolean,
        val b2: Boolean
)

Ann(true || false, true || true) class MyClass

// EXPECTED: Ann[b1 = true: jet.Boolean, b2 = true: jet.Boolean]
