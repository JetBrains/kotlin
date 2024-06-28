// PLATFORM_DEPENDANT_METADATA
// ALLOW_AST_ACCESS
// NO_CHECK_SOURCE_VS_BINARY
// IGNORE_BACKEND_K1: JVM_IR, ANY
// LANGUAGE: +MultiPlatformProjects
// IGNORE K1
// ^mute KotlinpCompilerTestDataTest
// K1/K2 difference: KT-60820

// MODULE: common
// FILE: common.kt
package test

expect annotation class Anno4(vararg val t: String)
expect annotation class Anno5(vararg val t: String = [])
expect annotation class Anno6(vararg val t: String = ["a"])

// MODULE: platform()()(common)
// FILE: test.kt
package test

annotation class Anno(vararg val t: String)
annotation class Anno2(vararg val t: String = [])
annotation class Anno3(vararg val t: String = ["a"])
actual annotation class Anno4(vararg val t: String)
actual annotation class Anno5(vararg val t: String)
actual annotation class Anno6(vararg val t: String)

@Anno("live", "long") fun foo() {}

@field:Anno("prosper") val bar = { 42 }()

@Anno() @Anno2() @Anno3() @Anno4() @Anno5() @Anno6() fun baz() {}
