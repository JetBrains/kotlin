// IGNORE_FIR_DIAGNOSTICS
// RUN_PIPELINE_TILL: BACKEND
// MODULE: common

package test

expect sealed class <!PACKAGE_OR_CLASSIFIER_REDECLARATION!>Sealed<!>
expect class <!PACKAGE_OR_CLASSIFIER_REDECLARATION!>Sealed1<!> : Sealed

expect sealed interface <!PACKAGE_OR_CLASSIFIER_REDECLARATION!>SealedIface<!>
expect class <!PACKAGE_OR_CLASSIFIER_REDECLARATION!>SealedImpl1<!> : SealedIface

// MODULE: intermediate()()(common)

package test

actual sealed class <!PACKAGE_OR_CLASSIFIER_REDECLARATION!>Sealed<!>(val v: Int)
actual class <!PACKAGE_OR_CLASSIFIER_REDECLARATION!>Sealed1<!>() : Sealed(1)
expect class <!PACKAGE_OR_CLASSIFIER_REDECLARATION!>Sealed2<!> : Sealed

actual sealed interface <!PACKAGE_OR_CLASSIFIER_REDECLARATION!>SealedIface<!>
actual class <!PACKAGE_OR_CLASSIFIER_REDECLARATION!>SealedImpl1<!>() : SealedIface
expect class <!PACKAGE_OR_CLASSIFIER_REDECLARATION!>SealedImpl2<!> : SealedIface

// MODULE: main()()(intermediate)

package test

actual class <!AMBIGUOUS_EXPECTS!>Sealed2<!>() : <!SEALED_INHERITOR_IN_DIFFERENT_MODULE!>Sealed<!>(2)
actual class <!AMBIGUOUS_EXPECTS!>SealedImpl2<!>() : <!SEALED_INHERITOR_IN_DIFFERENT_MODULE!>SealedIface<!>
