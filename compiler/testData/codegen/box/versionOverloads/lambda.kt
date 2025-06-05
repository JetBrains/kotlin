// TARGET_BACKEND: JVM
// IGNORE_BACKEND_K1: ANY
// CHECK_BYTECODE_LISTING
// WITH_STDLIB
@file:OptIn(ExperimentalStdlibApi::class)

fun foo(
    x: String,
    @IntroducedAt("1") y: (String) -> String = { it },
    @IntroducedAt("2") z: () -> String = { x }
) = z()

fun box() = foo("OK")