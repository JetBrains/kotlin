// RUN_PIPELINE_TILL: BACKEND
// LANGUAGE: +MultiPlatformProjects +AllowAnyAsAnActualTypeForExpectInterface

// MODULE: lib-common
// FILE: common.kt
expect interface Marker

// MODULE: lib-platform()()(lib-common)
// FILE: main.kt
actual typealias <!ACTUAL_WITHOUT_EXPECT!>Marker<!> = Any


// MODULE: app-common(lib-common)


// MODULE: app-platform(lib-platform)()(app-common)
open class B : <!SUPERTYPE_NOT_INITIALIZED!>Marker<!> {}
class C : B(), <!MANY_CLASSES_IN_SUPERTYPE_LIST, SUPERTYPE_NOT_INITIALIZED!>Marker<!> {}

interface Marker2: <!INTERFACE_WITH_SUPERCLASS!>Marker<!>
interface Marker3: Marker2, <!INTERFACE_WITH_SUPERCLASS!>Marker<!>

/* GENERATED_FIR_TAGS: actual, classDeclaration, expect, typeAliasDeclaration */
