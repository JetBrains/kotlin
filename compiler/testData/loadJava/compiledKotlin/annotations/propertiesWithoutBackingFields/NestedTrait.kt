package test

annotation class Anno

class Class {
    trait Trait {
        [Anno] val property: Int
    }
}
