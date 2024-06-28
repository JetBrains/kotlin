// LANGUAGE: +MultiPlatformProjects
// ISSUE: KT-66960

// MODULE: common
// FILE: common.kt
expect sealed class Base()

class CommonDerived : Base()

// should be an error
fun commonTest(x: Base) = <!NO_ELSE_IN_WHEN!>when<!> (x) {
    is CommonDerived -> 1
}

// MODULE: main()()(common)
// FILE: test.kt
actual typealias Base = PlatformBase

sealed class PlatformBase

class PlatformDerived : PlatformBase()

// should be ok
fun platformTest_1(x: Base) = when (x) {
    is CommonDerived -> 1
    is PlatformDerived -> 2
}

// should be an error
fun platformTest_2(x: Base) = when (x) {
    is PlatformDerived -> 2
}
