package test

annotation class Anno

class Class {
    default object {
        [Anno] var property: Int = 42
    }
}
