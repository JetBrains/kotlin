// PLATFORM_DEPENDANT_METADATA
package test

annotation class Anno

var property: Int = 42
    @[Anno] set(value) { }
