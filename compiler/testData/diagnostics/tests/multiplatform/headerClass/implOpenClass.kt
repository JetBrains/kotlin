// !LANGUAGE: +MultiPlatformProjects
// MODULE: m1-common
// FILE: common.kt

header class Foo

header fun getFoo(): Foo

// MODULE: m2-jvm(m1-common)
// FILE: jvm.kt

impl open class Foo

class Bar : Foo()

impl fun getFoo(): Foo = Bar()
