// LL_FIR_DIVERGENCE
// The compiler reports `PACKAGE_OR_CLASSIFIER_REDECLARATION` on both `A`s, but the LL FIR APIs output is also workable. The underlying
// issue is already reported: KTIJ-23371.
// LL_FIR_DIVERGENCE

// FIR_IDENTICAL
// FILE: f1.kt
package test

class A
class F1

// FILE: f2.kt
package test

class <!PACKAGE_OR_CLASSIFIER_REDECLARATION!>A<!>
class F2