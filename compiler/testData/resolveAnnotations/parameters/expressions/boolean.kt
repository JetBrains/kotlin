package test

annotation class Ann(
        val b1: Boolean,
        val b2: Boolean,
        val b3: Boolean
)

Ann(true and false, false or true, true xor false) class MyClass

// EXPECTED: Ann[b1 = false: jet.Boolean, b2 = true: jet.Boolean, b3 = true: jet.Boolean]
