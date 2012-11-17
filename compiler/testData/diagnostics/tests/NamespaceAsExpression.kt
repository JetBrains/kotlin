// FILE: b.kt
package root.a

// FILE: b.kt
package root

val x = <!EXPRESSION_EXPECTED_NAMESPACE_FOUND, DEBUG_INFO_ERROR_ELEMENT!>a<!>
val y2 = <!NAMESPACE_IS_NOT_AN_EXPRESSION!>package<!>