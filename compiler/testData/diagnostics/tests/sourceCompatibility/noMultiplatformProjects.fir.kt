expect fun foo1()
expect val bar1 = <!EXPECTED_PROPERTY_INITIALIZER!>42<!>
expect class Baz1

actual fun foo2() = 42
<!MUST_BE_INITIALIZED!>actual val bar2: Int<!>
actual interface Baz2
