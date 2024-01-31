// FIR_IDENTICAL
// MODULE: common
// TARGET_PLATFORM: Common

package test

sealed class SealedExpectActual(val v: Int)
class SealedExpectActual1() : SealedExpectActual(1)

// MODULE: intermediate()()(common)
// TARGET_PLATFORM: Common

package test

/* sealed inheritors are allowed in dependsOn source sets only for expect delclations */
class SealedExpectActual2() : <!SEALED_INHERITOR_IN_DIFFERENT_MODULE!>SealedExpectActual<!>(2)

// MODULE: main()()(intermediate)
// TARGET_PLATFORM: JVM

package test

/* sealed inheritors are allowed in dependsOn source sets only for expect delclations */
class SealedExpectActual3() : <!SEALED_INHERITOR_IN_DIFFERENT_MODULE!>SealedExpectActual<!>(3)
