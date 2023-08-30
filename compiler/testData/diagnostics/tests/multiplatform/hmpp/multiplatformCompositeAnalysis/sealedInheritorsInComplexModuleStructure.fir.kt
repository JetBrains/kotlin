
// MODULE: common
// TARGET_PLATFORM: Common

package foo

expect sealed class SealedWithSharedActual()
expect sealed class SealedWithPlatformActuals : SealedWithSharedActual

// MODULE: intermediate()()(common)
// TARGET_PLATFORM: Common
package foo

actual sealed class SealedWithSharedActual
class SimpleShared : <!UNRESOLVED_REFERENCE!>SealedWithPlatformActuals<!>()

// MODULE: main()()(intermediate)
// TARGET_PLATFORM: JVM
package foo

actual sealed class SealedWithPlatformActuals <!ACTUAL_WITHOUT_EXPECT!>actual constructor()<!>: <!SEALED_INHERITOR_IN_DIFFERENT_MODULE!>SealedWithSharedActual<!>()
