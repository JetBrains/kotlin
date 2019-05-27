// IGNORE_BACKEND: JVM_IR
package test

annotation class Anno

class Class {
    interface Trait {
        @[Anno] val property: Int
    }
}
