// RUN_PIPELINE_TILL: FRONTEND
// FILE: a.kt
package a.b

// FILE: b.kt
class <!PACKAGE_OR_CLASSIFIER_REDECLARATION!>a<!> {
    class b
}
