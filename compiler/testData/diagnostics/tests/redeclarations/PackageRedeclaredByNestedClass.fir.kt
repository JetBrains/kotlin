// RUN_PIPELINE_TILL: FRONTEND
// FILE: a.kt
package <!PACKAGE_CONFLICTS_WITH_CLASSIFIER!>a.b<!>

// FILE: b.kt
class a {
    class b
}
