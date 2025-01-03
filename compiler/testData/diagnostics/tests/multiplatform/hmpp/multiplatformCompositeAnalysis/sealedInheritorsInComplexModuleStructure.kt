// IGNORE_FIR_DIAGNOSTICS
// RUN_PIPELINE_TILL: BACKEND

// MODULE: common

package foo

expect sealed class <!PACKAGE_OR_CLASSIFIER_REDECLARATION!>SealedWithSharedActual<!>()
expect sealed class <!PACKAGE_OR_CLASSIFIER_REDECLARATION!>SealedWithPlatformActuals<!>() : SealedWithSharedActual

// MODULE: intermediate()()(common)
package foo

actual sealed class <!PACKAGE_OR_CLASSIFIER_REDECLARATION!>SealedWithSharedActual<!>
class <!PACKAGE_OR_CLASSIFIER_REDECLARATION!>SimpleShared<!> : SealedWithPlatformActuals()

// MODULE: main()()(intermediate)
package foo

actual sealed class <!AMBIGUOUS_EXPECTS!>SealedWithPlatformActuals<!> actual constructor(): <!SEALED_INHERITOR_IN_DIFFERENT_MODULE!>SealedWithSharedActual<!>()
