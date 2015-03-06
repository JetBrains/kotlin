package test

annotation class Anno

class Class {
    default object {
        [Anno] val property: Int
            get() = 42
    }
}
