// !LANGUAGE: +MultiPlatformProjects
// MODULE: m1-common
// FILE: common.kt

platform interface Interface

platform annotation class Anno(val prop: String)

platform object Object

platform class Class

platform enum class En { ENTRY }

// MODULE: m2-jvm(m1-common)
// FILE: jvm.kt

impl interface Interface

impl annotation class Anno(val prop: String)

impl object Object

impl class Class

impl enum class En { ENTRY }
