// !LANGUAGE: +MultiPlatformProjects
// MODULE: m1-common
// FILE: common.kt

header enum class En {
    E1,
    <!HEADER_ENUM_ENTRY_WITH_BODY!>E2 {
        <!HEADER_DECLARATION_WITH_BODY!>fun foo()<!> = ""
    },<!>
    <!HEADER_ENUM_ENTRY_WITH_BODY!>E3 { };<!>
}
