// MODULE: m1-common
// FILE: common.kt

expect annotation class Ann() {
    annotation class Nested()
}

@Ann.Nested
expect fun foo()

// MODULE: m1-jvm()()(m1-common)
// FILE: jvm.kt
annotation class AnnImpl {
    annotation class Nested
}

actual typealias Ann = AnnImpl

@AnnImpl.Nested
actual fun foo() {}
