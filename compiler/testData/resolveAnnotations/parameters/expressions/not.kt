package test

annotation class Ann(
        val b1: Boolean,
        val b2: Boolean,
        val b3: Boolean
)

@Ann(!true, !false) class MyClass

// EXPECTED: @Ann(b1 = false, b2 = true)
