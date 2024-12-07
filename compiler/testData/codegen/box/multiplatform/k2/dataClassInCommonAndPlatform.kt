// LANGUAGE: +MultiPlatformProjects
// ISSUE: KT-61972

// MODULE: common
// FILE: common.kt
data class CommonData(val value: String)

// MODULE: main()()(common)
// FILE: test.kt
data class PlatformData(val commonData: CommonData)

fun box() = "OK"
