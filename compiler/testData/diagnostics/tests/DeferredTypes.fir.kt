// NI_EXPECTED_FILE

interface T {
  val a = <!UNRESOLVED_REFERENCE!>Foo<!>.<!UNRESOLVED_REFERENCE!>bar<!>()
}