// PLATFORM_DEPENDANT_METADATA
package test

annotation class Anno

val property: Int
    @[Anno] get() = 42
