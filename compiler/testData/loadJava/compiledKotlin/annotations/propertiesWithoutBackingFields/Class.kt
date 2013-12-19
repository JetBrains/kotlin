package test

annotation class Anno

class Class {
    [Anno] val property: Int
        get() = 42
}
