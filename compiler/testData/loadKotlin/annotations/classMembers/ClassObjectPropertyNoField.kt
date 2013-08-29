package test

annotation class Anno

class Class {
    class object {
        [Anno] val property: Int
            get() = 42
    }
}
