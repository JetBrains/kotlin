// !LANGUAGE: +MultiPlatformProjects

// MODULE: common
// TARGET_PLATFORM: Common

package foo

expect sealed class SealedWithSharedActual()
expect sealed class SealedWithPlatformActuals : SealedWithSharedActual

// MODULE: intermediate()()(common)
// TARGET_PLATFORM: Common
package foo

actual sealed class SealedWithSharedActual
class SimpleShared : SealedWithPlatformActuals()

// MODULE: main()()(intermediate)
// TARGET_PLATFORM: JVM
package foo

actual sealed class SealedWithPlatformActuals <!NON_PRIVATE_OR_PROTECTED_CONSTRUCTOR_IN_SEALED!>actual constructor()<!>: SealedWithSharedActual()
