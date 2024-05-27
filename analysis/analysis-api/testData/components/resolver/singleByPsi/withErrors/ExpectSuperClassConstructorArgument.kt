// LANGUAGE: +MultiPlatformProjects

// MODULE: common
// TARGET_PLATFORM: Common

// FILE: Common.kt

expect open class Foo(val obj: Any)

expect class Bar(obj: Any) : Foo(ob<caret>j)