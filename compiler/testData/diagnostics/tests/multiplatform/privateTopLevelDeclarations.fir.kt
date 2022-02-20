// MODULE: m1-common
// FILE: common.kt

<!EXPECTED_PRIVATE_DECLARATION!>private<!> expect fun foo()
<!EXPECTED_PRIVATE_DECLARATION!>private<!> expect val bar: String
<!EXPECTED_PRIVATE_DECLARATION!>private<!> expect fun Int.memExt(): Any

private expect class Foo

// MODULE: m2-jvm()()(m1-common)
// FILE: jvm.kt

private actual fun foo() {}
private actual val bar: String = ""
private actual fun Int.memExt(): Any = 0

private actual class Foo
