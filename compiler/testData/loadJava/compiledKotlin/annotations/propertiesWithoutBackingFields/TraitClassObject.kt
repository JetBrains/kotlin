package test

annotation class Anno

trait Trait {
    default object {
        [Anno] val property: Int
            get() = 42
    }
}
