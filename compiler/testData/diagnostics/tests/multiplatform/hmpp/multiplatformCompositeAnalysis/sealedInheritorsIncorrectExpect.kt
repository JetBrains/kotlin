// FIR_IDENTICAL
// MODULE: common
// TARGET_PLATFORM: Common

package test

expect sealed class Sealed
expect class Sealed1 : Sealed

expect sealed interface SealedIface
expect class SealedImpl1 : SealedIface

// MODULE: intermediate()()(common)
// TARGET_PLATFORM: Common

package test

actual sealed class Sealed(val v: Int)
actual class Sealed1() : Sealed(1)
expect class Sealed2 : Sealed

actual sealed interface SealedIface
actual class SealedImpl1() : SealedIface
expect class SealedImpl2 : SealedIface

// MODULE: main()()(intermediate)
// TARGET_PLATFORM: JVM

package test

actual class Sealed2() : <!SEALED_INHERITOR_IN_DIFFERENT_MODULE!>Sealed<!>(2)
actual class SealedImpl2() : <!SEALED_INHERITOR_IN_DIFFERENT_MODULE!>SealedIface<!>
