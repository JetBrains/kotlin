// !LANGUAGE: +MultiPlatformProjects
// MODULE: m1-common
// FILE: common.kt

header interface Interface

header annotation class Anno(val prop: String)

header object Object

header class Class

header enum class En { ENTRY }

// MODULE: m2-jvm(m1-common)
// FILE: jvm.kt

impl interface Interface

impl annotation class Anno impl constructor(impl val prop: String)

impl object Object

impl class Class

impl enum class En { ENTRY }
