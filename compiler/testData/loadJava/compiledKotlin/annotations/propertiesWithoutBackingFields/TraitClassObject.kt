package test

annotation class Anno

interface Trait {
    companion object {
        @[Anno] val property: Int
            get() = 42
    }
}
