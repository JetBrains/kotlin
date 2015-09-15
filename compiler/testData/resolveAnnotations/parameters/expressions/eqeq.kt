package test

annotation class Ann(
        val b1: Boolean,
        val b2: Boolean,
        val b3: Boolean,
        val b4: Boolean,
        val b5: Boolean
)

@Ann(1 == 2, 1.0 == 2.0, 'b' == 'a', "a" == "b", "a" == "a") class MyClass

// EXPECTED: @Ann(b1 = false, b2 = false, b3 = false, b4 = false, b5 = true)
