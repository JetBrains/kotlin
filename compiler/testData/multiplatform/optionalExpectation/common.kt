// WITH_RUNTIME
// ADDITIONAL_COMPILER_ARGUMENTS: -Xopt-in=kotlin.ExperimentalMultiplatform

@OptionalExpectation
expect annotation class A()

expect class C {
    @A
    fun f()
}
