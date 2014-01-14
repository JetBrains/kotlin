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

// EXPECTED: Ann(b1 = false: Boolean, b2 = false: Boolean, b3 = true: Boolean, b4 = true: Boolean, b5 = true: Boolean, b6 = false: Boolean)
