package test

annotation class Anno

trait Trait {
    class object {
        [Anno] val property: Int
            get() = 42
    }
}
