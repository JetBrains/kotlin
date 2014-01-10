// FILE: b.kt
package root.a

// FILE: b.kt
package root

val x = <!EXPRESSION_EXPECTED_PACKAGE_FOUND!>a<!>
val y2 = <!PACKAGE_IS_NOT_AN_EXPRESSION!>package<!>