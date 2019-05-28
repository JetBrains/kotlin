package test

annotation class Anno

interface Trait {
    @[Anno] val property: Int
}
