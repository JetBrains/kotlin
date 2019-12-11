// !LANGUAGE: +MultiPlatformProjects
// MODULE: m1-common
// FILE: common.kt

expect enum class En {
    E1,
    E2 {
        fun foo() = ""
    },
    E3 { };
}
