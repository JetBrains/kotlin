// WITH_STDLIB
// ADDITIONAL_COMPILER_ARGUMENTS: -opt-in=kotlin.ExperimentalMultiplatform

@OptionalExpectation
expect annotation class A()

class C {
    @A
    fun f() {}
}
