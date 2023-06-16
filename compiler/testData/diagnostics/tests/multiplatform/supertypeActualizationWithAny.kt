// FIR_IDENTICAL
// MODULE: m1-common
expect open class A
expect class B : A

// MODULE: m1-jvm()()(m1-common)
actual typealias A = Any
actual class B
