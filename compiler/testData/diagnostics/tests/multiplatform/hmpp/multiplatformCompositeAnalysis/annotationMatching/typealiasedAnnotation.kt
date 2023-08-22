// FIR_IDENTICAL
// MODULE: common
// TARGET_PLATFORM: Common
expect annotation class Test()

@Test
expect fun unexpandedOnActual()

@Test
expect fun expandedOnActual()

// MODULE: main()()(common)
annotation class JunitTestInLib

actual typealias Test = JunitTestInLib

@Test
actual fun unexpandedOnActual() {}

@JunitTestInLib
actual fun expandedOnActual() {}

