// RUN_PIPELINE_TILL: BACKEND
// LANGUAGE: +MultiPlatformProjects +AllowAnyAsAnActualTypeForExpectInterface

// MODULE: lib-common
// FILE: common.kt
expect interface Marker

// MODULE: lib-platform()()(lib-common)
// FILE: main.kt
actual typealias Marker = Any


// MODULE: app-common(lib-common)


// MODULE: app-platform(lib-platform)()(app-common)
open class B : Marker {}
class C : B(), Marker {}

interface Marker2: Marker
interface Marker3: Marker2, Marker

/* GENERATED_FIR_TAGS: actual, classDeclaration, expect, typeAliasDeclaration */
