// LANGUAGE: +MultiPlatformProjects
// WITH_STDLIB
// DIAGNOSTICS: +ACTUAL_ANNOTATIONS_NOT_MATCH_EXPECT
// ISSUE: KT-82022

// MODULE: common
// FILE: common.kt

@kotlin.jvm.JvmInline
expect value class Some(val s: String)

// MODULE: intermediate()()(common)
// FILE: intermediate.kt

@kotlin.jvm.JvmInline
<!ACTUAL_ANNOTATIONS_NOT_MATCH_EXPECT{JS}!>actual<!> value class Some(val s: String)

// MODULE: platform()()(intermediate)
// FILE: platform.kt

fun box(): String {
    val s = Some("OK")
    return s.s
}
