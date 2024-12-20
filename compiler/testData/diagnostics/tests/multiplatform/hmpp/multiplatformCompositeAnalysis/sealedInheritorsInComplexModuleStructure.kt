// IGNORE_FIR_DIAGNOSTICS
// RUN_PIPELINE_TILL: FRONTEND
// FIR_IDENTICAL

// MODULE: common

package foo

expect sealed class SealedWithSharedActual()
expect sealed class SealedWithPlatformActuals() : SealedWithSharedActual

// MODULE: intermediate()()(common)
package foo

actual sealed class SealedWithSharedActual
class SimpleShared : SealedWithPlatformActuals()

// MODULE: main()()(intermediate)
package foo

actual sealed class SealedWithPlatformActuals actual constructor(): <!SEALED_INHERITOR_IN_DIFFERENT_MODULE!>SealedWithSharedActual<!>()
