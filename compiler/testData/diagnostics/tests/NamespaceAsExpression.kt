// FILE: b.kt
package root.a

// FILE: b.kt
package root

val x = <!EXPRESSION_EXPECTED_NAMESPACE_FOUND!>a<!>
val y2 = <!NAMESPACE_IS_NOT_AN_EXPRESSION!>package<!>
