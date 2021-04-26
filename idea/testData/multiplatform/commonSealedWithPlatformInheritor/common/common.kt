// ISSUE: KT-45848

sealed class <!LINE_MARKER("descr='Is subclassed by Derived PlatfromDerived'")!>Base<!>

class Derived : Base()

fun test_1(b: Base) = <!NO_ELSE_IN_WHEN{JVM}!>when<!> (b) {
    is Derived -> 1
}
