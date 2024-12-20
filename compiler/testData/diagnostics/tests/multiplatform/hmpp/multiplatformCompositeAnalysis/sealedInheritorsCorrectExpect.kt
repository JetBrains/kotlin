// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
// MODULE: common

package test

expect sealed class Sealed
expect class Sealed1 : Sealed

expect sealed interface SealedIface
expect class SealedImpl1 : SealedIface

// MODULE: intermediate()()(common)

package test

expect class Sealed2 : Sealed

expect class SealedImpl2 : SealedIface

// MODULE: main()()(intermediate)

package test

actual sealed class Sealed(val v: Int)
actual class Sealed1() : Sealed(1)
actual class Sealed2() : Sealed(2)

actual sealed interface SealedIface
actual class SealedImpl1() : SealedIface
actual class SealedImpl2() : SealedIface
