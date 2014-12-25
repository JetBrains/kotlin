// FILE: b.kt
package root.a

// FILE: b.kt
package root

val x = root.<!EXPRESSION_EXPECTED_PACKAGE_FOUND!>a<!>
val y2 = <!PACKAGE_IS_NOT_AN_EXPRESSION!>package<!>