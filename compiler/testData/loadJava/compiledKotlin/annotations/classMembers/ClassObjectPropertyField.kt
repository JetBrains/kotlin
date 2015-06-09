package test

annotation class Anno

class Class {
    companion object {
        @[Anno] var property: Int = 42
    }
}
