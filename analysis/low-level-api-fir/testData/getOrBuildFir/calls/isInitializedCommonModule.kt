// SKIP_WHEN_OUT_OF_CONTENT_ROOT
// MODULE: main-common
// WITH_STDLIB
// FILE: main.kt
lateinit var myLateinit: String

fun check() {
    <expr>::myLateinit.isInitialized</expr>
}
