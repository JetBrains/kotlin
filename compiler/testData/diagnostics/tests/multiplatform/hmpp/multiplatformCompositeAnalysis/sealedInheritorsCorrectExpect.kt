// RUN_PIPELINE_TILL: BACKEND
// MODULE: common

package test

expect sealed class <!PACKAGE_OR_CLASSIFIER_REDECLARATION!>Sealed<!>
expect class <!PACKAGE_OR_CLASSIFIER_REDECLARATION!>Sealed1<!> : Sealed

expect sealed interface <!PACKAGE_OR_CLASSIFIER_REDECLARATION!>SealedIface<!>
expect class <!PACKAGE_OR_CLASSIFIER_REDECLARATION!>SealedImpl1<!> : SealedIface

// MODULE: intermediate()()(common)

package test

expect class <!PACKAGE_OR_CLASSIFIER_REDECLARATION!>Sealed2<!> : Sealed

expect class <!PACKAGE_OR_CLASSIFIER_REDECLARATION!>SealedImpl2<!> : SealedIface

// MODULE: main()()(intermediate)

package test

actual sealed class <!AMBIGUOUS_EXPECTS!>Sealed<!>(val v: Int)
actual class <!AMBIGUOUS_EXPECTS!>Sealed1<!>() : Sealed(1)
actual class <!AMBIGUOUS_EXPECTS!>Sealed2<!>() : Sealed(2)

actual sealed interface <!AMBIGUOUS_EXPECTS!>SealedIface<!>
actual class <!AMBIGUOUS_EXPECTS!>SealedImpl1<!>() : SealedIface
actual class <!AMBIGUOUS_EXPECTS!>SealedImpl2<!>() : SealedIface
