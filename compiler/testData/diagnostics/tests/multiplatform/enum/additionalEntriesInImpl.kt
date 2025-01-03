// RUN_PIPELINE_TILL: BACKEND
// MODULE: m1-common
// FILE: common.kt
expect enum class <!PACKAGE_OR_CLASSIFIER_REDECLARATION!>Foo<!> { A, B }
expect enum class <!PACKAGE_OR_CLASSIFIER_REDECLARATION!>Bar<!> { X, Y, Z }

// MODULE: m2-jvm()()(m1-common)
// FILE: jvm.kt
actual enum class Foo { A, B, C, D, E }
actual enum class Bar { V, X, W, Y, Z }
