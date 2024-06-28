// PLATFORM_DEPENDANT_METADATA
package test

annotation class Anno

class Class {
    val property: Int
        @[Anno] get() = 42
}
