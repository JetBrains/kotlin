package test

annotation class Anno

class Class {
    interface Trait {
        @[Anno] val property: Int
    }
}
