package test

annotation class Anno

trait Trait {
    companion object {
        [Anno] val property: Int
            get() = 42
    }
}
