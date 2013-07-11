package test

annotation class Anno

class Class {
    var property: Int = 42
        [Anno] set(value) { }
}
