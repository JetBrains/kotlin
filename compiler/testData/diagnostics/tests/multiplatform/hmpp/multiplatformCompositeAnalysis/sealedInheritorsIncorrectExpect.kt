// IGNORE_FIR_DIAGNOSTICS
// RUN_PIPELINE_TILL: FRONTEND
// FIR_IDENTICAL
// MODULE: common

package test

expect sealed class Sealed
expect class Sealed1 : Sealed

expect sealed interface SealedIface
expect class SealedImpl1 : SealedIface

// MODULE: intermediate()()(common)

package test

actual sealed class Sealed(val v: Int)
actual class Sealed1() : Sealed(1)
expect class Sealed2 : Sealed

actual sealed interface SealedIface
actual class SealedImpl1() : SealedIface
expect class SealedImpl2 : SealedIface

// MODULE: main()()(intermediate)

package test

actual class Sealed2() : <!SEALED_INHERITOR_IN_DIFFERENT_MODULE!>Sealed<!>(2)
actual class SealedImpl2() : <!SEALED_INHERITOR_IN_DIFFERENT_MODULE!>SealedIface<!>
