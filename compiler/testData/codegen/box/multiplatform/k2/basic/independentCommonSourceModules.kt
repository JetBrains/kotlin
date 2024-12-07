// LANGUAGE: +MultiPlatformProjects

// MODULE: common1
// FILE: common1.kt

fun o() = "O"

// MODULE: common2
// The test framework adds additional files to common modules with no dependencies.
// Those files should only be added once, so we exclude them from common2 and only include
// them in common1.
// NO_COMMON_FILES
// FILE: common2.kt

fun k() = "K"

// MODULE: platform()()(common1, common2)
// FILE: platform.kt

fun box() = o() + k()
