// PLATFORM_DEPENDANT_METADATA
package test

annotation class Anno

class Class {
    companion object {
        @field:Anno var property: Int = 42
    }
}
