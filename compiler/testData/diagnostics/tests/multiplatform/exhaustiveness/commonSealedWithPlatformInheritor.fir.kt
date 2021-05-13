// !LANGUAGE: +MultiPlatformProjects
// ISSUE: KT-45848
// MODULE: m1-common

sealed class Base

class Derived : Base()

fun test_1(b: Base) = when (b) {
    is Derived -> 1
}

// MODULE: m1-jvm()()(m1-common)

class PlatfromDerived : Base() // must be an error

fun test_2(b: Base) = when (b) {
    is Derived -> 1
}
