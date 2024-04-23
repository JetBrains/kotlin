// FILE: a.kt
package a
class b {}
// FILE: b.kt
package <!PACKAGE_CONFLICTS_WITH_CLASSIFIER!>a.b<!>
// FILE: c.kt
package <!PACKAGE_CONFLICTS_WITH_CLASSIFIER!>a.b<!>
