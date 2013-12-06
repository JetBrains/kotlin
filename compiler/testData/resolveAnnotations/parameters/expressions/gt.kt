package test

annotation class Ann(
        val b1: Boolean,
        val b2: Boolean,
        val b3: Boolean,
        val b4: Boolean,
        val b5: Boolean,
        val b6: Boolean
)

val a = 1
val b = 2

Ann(1 > 2, 1.0 > 2.0, 2 > a, b > a, 'b' > 'a', "a" > "b") class MyClass

// EXPECTED: Ann[b1 = false: jet.Boolean, b2 = false: jet.Boolean, b3 = true: jet.Boolean, b4 = true: jet.Boolean, b5 = true: jet.Boolean, b6 = false: jet.Boolean]
