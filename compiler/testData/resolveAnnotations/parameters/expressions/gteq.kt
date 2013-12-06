package test

annotation class Ann(
        val b1: Boolean,
        val b2: Boolean,
        val b3: Boolean,
        val b4: Boolean
)

Ann(1 >= 2, 1.0 >= 2.0, 1 >= 1, 1.0 >= 1.0) class MyClass

// EXPECTED: Ann[b1 = false: jet.Boolean, b2 = false: jet.Boolean, b3 = true: jet.Boolean, b4 = true: jet.Boolean]
