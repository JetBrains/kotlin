// IGNORE_FIR_DIAGNOSTICS
// RUN_PIPELINE_TILL: FIR2IR
// LANGUAGE: +MultiPlatformProjects +AllowAnyAsAnActualTypeForExpectInterface

// MODULE: common
// FILE: common.kt
expect interface Marker

expect interface NotMarker {
    val test: String
}

open class B : <!SUPERTYPE_NOT_INITIALIZED{JVM}!>Marker<!> {}
class C : B(), <!MANY_CLASSES_IN_SUPERTYPE_LIST{JVM}, SUPERTYPE_NOT_INITIALIZED{JVM}!>Marker<!> {}

interface Marker2: <!INTERFACE_WITH_SUPERCLASS{JVM}!>Marker<!>
interface Marker3: Marker2, <!INTERFACE_WITH_SUPERCLASS{JVM}!>Marker<!>

// MODULE: jvm()()(common)
// FILE: main.kt
actual typealias <!ACTUAL_WITHOUT_EXPECT!>Marker<!> = Any
actual typealias <!ACTUAL_WITHOUT_EXPECT!>NotMarker<!> = Any

/* GENERATED_FIR_TAGS: actual, classDeclaration, expect, typeAliasDeclaration */
