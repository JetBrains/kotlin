// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +MultiPlatformProjects
// ISSUE: KT-69726, KT-69727
// WITH_STDLIB

// MODULE: m1-common
// FILE: common.kt

expect enum class Enums1 {
    A123, B456;
}

expect enum class Enums2 {
    C123, D456;
}

fun useCloneCommon() {
    Enums2.C123.<!UNRESOLVED_REFERENCE!>clone<!>()
}

// MODULE: m2-js()()(m1-common)
// FILE: js.kt

actual enum class Enums1 {
    A123, B456;

    fun clone() {}
}

actual enum class Enums2 {
    C123, D456;
}

fun useCloneJs() {
    Enums2.C123.<!UNRESOLVED_REFERENCE!>clone<!>()
}
