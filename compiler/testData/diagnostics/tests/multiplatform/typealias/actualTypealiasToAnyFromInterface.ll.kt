// IGNORE_FIR_DIAGNOSTICS
// RUN_PIPELINE_TILL: FIR2IR
// LANGUAGE: +MultiPlatformProjects +AllowAnyAsAnActualTypeForExpectInterface

// MODULE: common
// FILE: common.kt
expect interface Marker

expect interface NotMarker {
    val test: String
}

open class B : Marker {}
class C : B(), Marker {}

interface Marker2: Marker
interface Marker3: Marker2, Marker

// MODULE: jvm()()(common)
// FILE: main.kt
actual typealias Marker = Any
actual typealias <!NO_ACTUAL_CLASS_MEMBER_FOR_EXPECTED_CLASS!>NotMarker<!> = Any

/* GENERATED_FIR_TAGS: actual, classDeclaration, expect, typeAliasDeclaration */
